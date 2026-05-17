const jwt = require('jsonwebtoken');
const Message = require('../models/Message');
const Conversation = require('../models/Conversation');
const { getDB } = require('../config/database');
const { isChatEnabled } = require('../middleware/featureGate');
const { moderateMessage } = require('../services/safetyModerationService');
const { upsertConversationMetadata } = require('../services/chatMetadataService');
const logger = require('../utils/logger');
const makeChatId = (a, b) => [a, b].sort().join('_');
const MAX_MESSAGE_LENGTH = parseInt(process.env.MAX_CHAT_MESSAGE_LENGTH || '2000', 10);
const NOTIFICATION_API_URL = process.env.NOTIFICATION_API_URL;
const INTERNAL_SERVICE_SECRET = process.env.INTERNAL_SERVICE_SECRET;
const verifyOptions = () => ({
  issuer: process.env.JWT_ISSUER || 'soulmatch-auth',
  audience: process.env.JWT_AUDIENCE || 'soulmatch-api'
});

const buildMessagePreview = (type, content) => {
  if (type && type !== 'text') return 'You received a new ' + type + ' message on SoulMatch.';
  const text = String(content || '').trim();
  if (!text) return 'You received a new message on SoulMatch.';
  return text.length > 120 ? text.slice(0, 117) + '...' : text;
};

const sendChatNotification = async ({ receiverId, senderId, chatId, type, content }) => {
  if (!NOTIFICATION_API_URL || !INTERNAL_SERVICE_SECRET || typeof fetch !== 'function') return;
  try {
    const response = await fetch(NOTIFICATION_API_URL.replace(/\/$/, '') + '/send', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-internal-service-secret': INTERNAL_SERVICE_SECRET
      },
      body: JSON.stringify({
        userId: receiverId,
        title: 'New message',
        body: buildMessagePreview(type, content),
        data: {
          type: 'chat_message',
          chatId,
          senderUserId: senderId
        }
      })
    });
    if (!response.ok) {
      logger.warn('Chat notification push failed with status ' + response.status);
    }
  } catch (err) {
    logger.warn('Chat notification push failed: ' + err.message);
  }
};

const getProfileIdByUserId = async (db, userId) => {
  const res = await db.query(
    `SELECT p.profile_id
     FROM profiles p
     JOIN users u ON u.user_id=p.user_id
     WHERE p.user_id=$1
       AND COALESCE(p.profile_status,'active')='active'
       AND COALESCE(p.admin_status,'active')='active'
       AND u.is_active=true
       AND COALESCE(u.is_banned,false)=false
     LIMIT 1`,
    [userId]
  );
  return res.rows[0]?.profile_id || null;
};
const recordSafetyEvent = async ({ senderId, receiverId, chatId, moderation }) => {
  if (!moderation?.flags?.length) return;
  try {
    const db = await getDB();
    await db.query(
      `INSERT INTO analytics_events (event_type, service_name, user_id, payload, created_at)
       VALUES ('chat_message_safety','chat-service',$1,$2::jsonb,NOW())`,
      [
        senderId,
        JSON.stringify({
          receiverId,
          chatId,
          action: moderation.action,
          severity: moderation.severity,
          flags: moderation.flags,
          provider: moderation.provider,
          model: moderation.model
        })
      ]
    );
  } catch (error) {
    logger.warn('Chat safety analytics failed: ' + error.message);
  }
};
const canChat = async (u1, u2) => {
  const db = await getDB();
  const [p1, p2] = await Promise.all([getProfileIdByUserId(db, u1), getProfileIdByUserId(db, u2)]);
  if (!p1 || !p2) return false;
  const safety = await db.query(
    `SELECT
       EXISTS(
         SELECT 1 FROM blocks
         WHERE (blocker_id=$1 AND blocked_id=$2) OR (blocker_id=$2 AND blocked_id=$1)
       ) AS blocked,
       EXISTS(
         SELECT 1 FROM users
         WHERE user_id = ANY($3::uuid[]) AND (is_banned=true OR is_active=false)
       ) AS banned,
       COALESCE((SELECT COUNT(*)::int FROM reports WHERE reported_id IN ($1,$2) AND status IN ('pending','open','reviewing')), 0) AS open_reports`,
    [u1, u2, [u1, u2]]
  );
  if (safety.rows[0]?.blocked || safety.rows[0]?.banned || Number(safety.rows[0]?.open_reports || 0) >= 3) return false;
  const res = await db.query("SELECT EXISTS(SELECT 1 FROM interests WHERE ((sender_id=$1 AND receiver_id=$2) OR (sender_id=$2 AND receiver_id=$1)) AND status='accepted') AS ok", [p1,p2]);
  return res.rows[0]?.ok || false;
};
const isConversationParticipant = async (chatId, userId) => {
  return !!(await Conversation.exists({ chatId, participants: userId }));
};
exports.setupSocketHandlers = (io) => {
  io.use(async (socket, next) => {
    if (!await isChatEnabled()) return next(new Error('Chat is temporarily unavailable'));
    const token = socket.handshake.auth?.token;
    if (!token) return next(new Error('Token required'));
    try { const d = jwt.verify(token, process.env.JWT_SECRET, verifyOptions()); socket.userId = d.userId; next(); }
    catch { next(new Error('Invalid token')); }
  });
  io.on('connection', (socket) => {
    logger.info('Socket connected: ' + socket.id);
    socket.join('user:' + socket.userId);
    socket.on('message:send', async (data, cb) => {
      try {
        if (!await isChatEnabled()) return cb && cb({ error: 'Chat is temporarily unavailable for maintenance' });
        const { receiverId, type, content, duration } = data;
        const resolvedType = type || 'text';
        if (!receiverId) return cb && cb({ error: 'Receiver is required' });
        if (resolvedType === 'text' && (!content || String(content).length > MAX_MESSAGE_LENGTH)) {
          return cb && cb({ error: 'Message must be between 1 and ' + MAX_MESSAGE_LENGTH + ' characters' });
        }
        if (!await canChat(socket.userId, receiverId)) return cb && cb({ error: 'Send interest first to chat' });
        const cid = makeChatId(socket.userId, receiverId);
        const moderation = resolvedType === 'text'
          ? await moderateMessage(content)
          : { action: 'allow', severity: 'none', flags: [], provider: 'rules', model: 'local' };
        await recordSafetyEvent({ senderId: socket.userId, receiverId, chatId: cid, moderation });
        if (moderation.action === 'block') {
          return cb && cb({
            error: 'This message was blocked for safety. Please keep conversations respectful and avoid financial, private photo, or pressure requests.',
            code: 'MESSAGE_BLOCKED',
            safetyFlags: moderation.flags
          });
        }
        const safetyFlags = moderation.flags || [];
        const msg = await Message.create({ chatId:cid, senderId:socket.userId, receiverId, type:resolvedType, content, duration, safetyFlags });
        const conversation = await Conversation.findOneAndUpdate(
          { chatId:cid },
          {
            $set:{
              chatId:cid,
              lastMessage:{
                content:resolvedType === 'text' ? content : '[' + resolvedType + ']',
                type:resolvedType,
                sentAt:new Date(),
                senderId:socket.userId
              }
            },
            $addToSet:{ participants:{ $each:[socket.userId,receiverId] } },
            $inc:{ ['unreadCounts.'+receiverId]:1 }
          },
          { upsert:true, new:true }
        );
        const msgData = { messageId:msg._id.toString(), chatId:cid, senderId:socket.userId, receiverId, type:resolvedType, content, duration, sentAt:msg.sentAt, status:'sent' };
        await upsertConversationMetadata({
          chatId: cid,
          participants: conversation.participants || [socket.userId, receiverId],
          lastMessage: { type: resolvedType, content, sentAt: msg.sentAt, senderId: socket.userId },
          source: conversation.source || 'socket',
          interestId: conversation.interestId || null,
          incrementMessageCount: true
        });
        io.to('user:'+receiverId).emit('message:received', msgData);
        await sendChatNotification({ receiverId, senderId: socket.userId, chatId: cid, type: resolvedType, content });
        cb && cb({ success:true, message:msgData });
      } catch (err) { logger.error('message:send error: '+err.message); cb && cb({ error:'Failed to send' }); }
    });
    socket.on('message:read', async ({ chatId:cid }) => {
      try {
        if (!cid || !await isConversationParticipant(cid, socket.userId)) return;
        await Message.updateMany({ chatId:cid, receiverId:socket.userId, status:{ $ne:'read' } }, { status:'read', readAt:new Date() });
        await Conversation.findOneAndUpdate({ chatId:cid }, { $set:{ ['unreadCounts.'+socket.userId]:0 } });
        const senderId = cid.split('_').find(id => id !== socket.userId);
        if (senderId) io.to('user:'+senderId).emit('message:all_read', { chatId:cid });
      } catch (err) { logger.error('message:read error: '+err.message); }
    });
    socket.on('typing:start', async ({ receiverId }) => {
      if (receiverId && await canChat(socket.userId, receiverId)) io.to('user:'+receiverId).emit('typing:start', { senderId:socket.userId });
    });
    socket.on('typing:stop', async ({ receiverId }) => {
      if (receiverId && await canChat(socket.userId, receiverId)) io.to('user:'+receiverId).emit('typing:stop', { senderId:socket.userId });
    });
    socket.on('disconnect', () => logger.info('Socket disconnected: '+socket.id));
  });
};
