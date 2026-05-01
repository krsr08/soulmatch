const jwt = require('jsonwebtoken');
const Message = require('../models/Message');
const Conversation = require('../models/Conversation');
const { getDB } = require('../config/database');
const { isChatEnabled } = require('../middleware/featureGate');
const logger = require('../utils/logger');
const makeChatId = (a, b) => [a, b].sort().join('_');
const MAX_MESSAGE_LENGTH = parseInt(process.env.MAX_CHAT_MESSAGE_LENGTH || '2000', 10);
const getProfileIdByUserId = async (db, userId) => {
  const res = await db.query('SELECT profile_id FROM profiles WHERE user_id=$1 LIMIT 1', [userId]);
  return res.rows[0]?.profile_id || null;
};
const canChat = async (u1, u2) => {
  const db = await getDB();
  const [p1, p2] = await Promise.all([getProfileIdByUserId(db, u1), getProfileIdByUserId(db, u2)]);
  if (!p1 || !p2) return false;
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
    try { const d = jwt.verify(token, process.env.JWT_SECRET); socket.userId = d.userId; next(); }
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
        const msg = await Message.create({ chatId:cid, senderId:socket.userId, receiverId, type:resolvedType, content, duration });
        await Conversation.findOneAndUpdate(
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
        io.to('user:'+receiverId).emit('message:received', msgData);
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
