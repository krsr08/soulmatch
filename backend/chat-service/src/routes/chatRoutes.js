const express = require('express');
const Message = require('../models/Message');
const Conversation = require('../models/Conversation');
const { authenticate } = require('../middleware/authMiddleware');
const { ensureChatEnabled } = require('../middleware/featureGate');
const { getDB } = require('../config/database');

const router = express.Router();
const MAX_PAGE_SIZE = 100;

async function getProfileIdByUserId(db, userId) {
  const row = await db.query('SELECT profile_id FROM profiles WHERE user_id=$1 LIMIT 1', [userId]);
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
    const [currentProfileId, targetProfileId] = await Promise.all([
      getProfileIdByUserId(db, req.user.userId),
      getProfileIdByUserId(db, req.params.targetUserId)
    ]);
    if (!currentProfileId || !targetProfileId) {
      return res.json({ success: true, data: { canChat: false, reason: 'profile_not_found' } });
    }
    const blockedUserIds = await getBlockedUserIds(db, req.user.userId, [req.params.targetUserId]);
    if (blockedUserIds.has(req.params.targetUserId)) {
      return res.json({ success: true, data: { canChat: false, reason: 'blocked' } });
    }
    const r = await db.query("SELECT EXISTS(SELECT 1 FROM interests WHERE ((sender_id=$1 AND receiver_id=$2) OR (sender_id=$2 AND receiver_id=$1)) AND status='accepted') AS eligible", [currentProfileId, targetProfileId]);
    const eligible = r.rows[0]?.eligible || false;
    res.json({ success: true, data: { canChat: eligible, reason: eligible ? 'mutual_interest' : 'no_mutual_interest' } });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
