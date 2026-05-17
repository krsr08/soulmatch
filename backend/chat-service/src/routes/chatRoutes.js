const express = require('express');
const Message = require('../models/Message');
const Conversation = require('../models/Conversation');
const { authenticate } = require('../middleware/authMiddleware');
const { authenticateService } = require('../middleware/serviceAuthMiddleware');
const { ensureChatEnabled } = require('../middleware/featureGate');
const { getDB } = require('../config/database');
const { detectTextSafety } = require('../services/safetyModerationService');
const { touchConversationMetadata } = require('../services/chatMetadataService');
const crypto = require('crypto');

const router = express.Router();
const MAX_PAGE_SIZE = 100;
const makeChatId = (a, b) => [a, b].sort().join('_');

async function getProfileIdByUserId(db, userId) {
  const row = await db.query(
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
  return row.rows[0]?.profile_id || null;
}

async function getBlockedUserIds(db, currentUserId, otherUserIds) {
  const ids = [...new Set((otherUserIds || []).filter(Boolean))];
  if (!ids.length) return new Set();
  const rows = await db.query(
    `SELECT blocker_id, blocked_id
     FROM blocks
     WHERE (blocker_id=$1 AND blocked_id=ANY($2::uuid[]))
        OR (blocked_id=$1 AND blocker_id=ANY($2::uuid[]))`,
    [currentUserId, ids]
  );
  return new Set(rows.rows.map(row => row.blocker_id === currentUserId ? row.blocked_id : row.blocker_id));
}

async function getChatEligibility(db, currentUserId, targetUserId) {
  const [currentProfileId, targetProfileId] = await Promise.all([
    getProfileIdByUserId(db, currentUserId),
    getProfileIdByUserId(db, targetUserId)
  ]);
  if (!currentProfileId || !targetProfileId) return { canChat: false, reason: 'profile_not_found' };
  const blockedUserIds = await getBlockedUserIds(db, currentUserId, [targetUserId]);
  if (blockedUserIds.has(targetUserId)) return { canChat: false, reason: 'blocked' };
  const safety = await db.query(
    `SELECT
       EXISTS(SELECT 1 FROM users WHERE user_id = ANY($1::uuid[]) AND (is_banned=true OR is_active=false)) AS banned,
       COALESCE((SELECT COUNT(*)::int FROM reports WHERE reported_id=$2 AND status IN ('pending','open','reviewing')), 0) AS target_open_reports,
       COALESCE((SELECT COUNT(*)::int FROM reports WHERE reported_id=$3 AND status IN ('pending','open','reviewing')), 0) AS current_open_reports`,
    [[currentUserId, targetUserId], targetUserId, currentUserId]
  );
  if (safety.rows[0]?.banned) return { canChat: false, reason: 'restricted_user' };
  if (Number(safety.rows[0]?.target_open_reports || 0) >= 3 || Number(safety.rows[0]?.current_open_reports || 0) >= 3) {
    return { canChat: false, reason: 'safety_review' };
  }
  const r = await db.query(
    "SELECT EXISTS(SELECT 1 FROM interests WHERE ((sender_id=$1 AND receiver_id=$2) OR (sender_id=$2 AND receiver_id=$1)) AND status='accepted') AS eligible",
    [currentProfileId, targetProfileId]
  );
  const eligible = r.rows[0]?.eligible || false;
  return { canChat: eligible, reason: eligible ? 'mutual_interest' : 'no_mutual_interest' };
}

router.post('/messages/:messageId/report', authenticate, ensureChatEnabled, async (req, res, next) => {
  try {
    const message = await Message.findById(req.params.messageId).lean();
    if (!message || (message.senderId !== req.user.userId && message.receiverId !== req.user.userId)) {
      return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Message not found.' } });
    }
    const reportedUserId = message.senderId === req.user.userId ? message.receiverId : message.senderId;
    const db = await getDB();
    const flags = detectTextSafety(message.content).concat(Array.isArray(message.safetyFlags) ? message.safetyFlags : []);
    await db.query(
      `INSERT INTO chat_message_reports (
         chat_message_report_id,message_id,chat_id,reporter_user_id,reported_user_id,reason,description,safety_flags,status,created_at
       )
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8::jsonb,'pending',NOW())`,
      [
        crypto.randomUUID(),
        message._id.toString(),
        message.chatId,
        req.user.userId,
        reportedUserId,
        req.body?.reason || 'member_report',
        req.body?.description || '',
        JSON.stringify(flags)
      ]
    );
    await db.query(
      `INSERT INTO analytics_events (event_type, service_name, user_id, payload, created_at)
       VALUES ('chat_report','chat-service',$1,$2::jsonb,NOW())`,
      [req.user.userId, JSON.stringify({ messageId: message._id.toString(), chatId: message.chatId, reportedUserId, flags })]
    ).catch(() => {});
    res.json({ success: true, data: { status: 'pending' }, message: 'Message report submitted for safety review.' });
  } catch (err) {
    next(err);
  }
});

router.get('/conversations', authenticate, ensureChatEnabled, async (req, res, next) => {
  try {
    const convs = await Conversation.find({ participants: req.user.userId }).sort({ updatedAt: -1 }).limit(50).lean();
    const otherUserIds = [...new Set(convs.flatMap(conv => conv.participants.filter(id => id !== req.user.userId)))];
    const db = await getDB();
    const profileRows = otherUserIds.length
      ? await db.query('SELECT user_id,profile_id,first_name,last_name,primary_photo_url FROM profiles WHERE user_id = ANY($1::uuid[])', [otherUserIds])
      : { rows: [] };
    const profileMap = new Map(profileRows.rows.map(row => [row.user_id, row]));
    const blockedUserIds = await getBlockedUserIds(db, req.user.userId, otherUserIds);
    const data = convs.filter(conv => {
      const participantUserId = conv.participants.find(id => id !== req.user.userId) || '';
      return !blockedUserIds.has(participantUserId);
    }).map(conv => {
      const participantUserId = conv.participants.find(id => id !== req.user.userId) || '';
      const participant = profileMap.get(participantUserId);
      return {
        ...conv,
        participantUserId,
        participantProfileId: participant?.profile_id || '',
        participantName: participant ? `${participant.first_name || ''} ${participant.last_name || ''}`.trim() : 'Member',
        participantPhotoUrl: participant?.primary_photo_url || null,
        flowId: conv.flowId || null,
        flowVersionId: conv.flowVersionId || null,
        flowMessages: conv.flowMessages || [],
        flowMessagesJson: conv.flowMessagesJson || null,
        flowBusinessHourType: conv.flowBusinessHourType || null,
        serviceAccountId: conv.serviceAccountId || null,
        operatingHoursId: conv.operatingHoursId || null
      };
    });
    res.json({ success: true, data });
  } catch (err) {
    next(err);
  }
});

router.get('/:chatId/messages', authenticate, ensureChatEnabled, async (req, res, next) => {
  try {
    const conversation = await Conversation.findOne({
      chatId: req.params.chatId,
      participants: req.user.userId
    }).lean();
    if (!conversation) {
      return res.status(403).json({
        success: false,
        error: {
          code: 'FORBIDDEN',
          message: 'You are not a participant in this conversation.'
        }
      });
    }
    const otherUserId = conversation.participants.find(id => id !== req.user.userId);
    const db = await getDB();
    const blockedUserIds = await getBlockedUserIds(db, req.user.userId, [otherUserId]);
    if (blockedUserIds.has(otherUserId)) {
      return res.status(403).json({
        success: false,
        error: {
          code: 'FORBIDDEN',
          message: 'This conversation is blocked.'
        }
      });
    }
    const page = Math.max(parseInt(req.query.page, 10) || 1, 1);
    const requestedLimit = parseInt(req.query.limit, 10) || 50;
    const limit = Math.min(Math.max(requestedLimit, 1), MAX_PAGE_SIZE);
    const msgs = await Message.find({ chatId: req.params.chatId }).sort({ sentAt: -1 }).skip((page - 1) * limit).limit(limit);
    const messages = msgs.reverse().map(msg => ({
      messageId: msg._id.toString(),
      chatId: msg.chatId,
      senderId: msg.senderId,
      receiverId: msg.receiverId,
      type: msg.type || 'text',
      content: msg.content,
      status: msg.status || 'sent',
      sentAt: msg.sentAt,
      flowStepId: msg.flowStepId || null,
      alias: msg.alias || null,
      messageType: msg.messageType || null,
      messageUserType: msg.messageUserType || null,
      messageUserAlias: msg.messageUserAlias || null,
      createdMillis: msg.createdMillis || null
    }));
    res.json({ success: true, data: { messages, page, limit } });
  } catch (err) {
    next(err);
  }
});

router.get('/eligibility/:targetUserId', authenticate, ensureChatEnabled, async (req, res, next) => {
  try {
    const db = await getDB();
    res.json({ success: true, data: await getChatEligibility(db, req.user.userId, req.params.targetUserId) });
  } catch (err) {
    next(err);
  }
});

router.post('/internal/conversations', authenticateService, async (req, res, next) => {
  try {
    const participants = Array.isArray(req.body?.participants) ? req.body.participants.map(String).filter(Boolean) : [];
    if (participants.length !== 2 || participants[0] === participants[1]) {
      return res.status(400).json({
        success: false,
        error: {
          code: 'VALIDATION_ERROR',
          message: 'Exactly two distinct participants are required.'
        }
      });
    }
    const chatId = makeChatId(participants[0], participants[1]);
    const conversation = await Conversation.findOneAndUpdate(
      { chatId },
      {
        $setOnInsert: {
          chatId,
          unreadCounts: {},
          createdAt: new Date()
        },
        $set: {
          source: req.body?.source || 'matching-service',
          interestId: req.body?.interestId || null,
          updatedAt: new Date()
        },
        $addToSet: { participants: { $each: participants } }
      },
      { upsert: true, new: true }
    );
    await touchConversationMetadata(conversation);
    res.json({ success: true, data: { chatId, conversationId: conversation._id.toString() } });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
