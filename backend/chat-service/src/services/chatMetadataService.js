const { getDB } = require('../config/database');
const logger = require('../utils/logger');

const preview = (message = {}) => {
  if (!message) return null;
  const type = message.type || 'text';
  if (type !== 'text') return `[${type}]`;
  const text = String(message.content || '').trim();
  if (!text) return null;
  return text.length > 160 ? text.slice(0, 157) + '...' : text;
};

async function upsertConversationMetadata({
  chatId,
  participants = [],
  lastMessage = null,
  source = null,
  interestId = null,
  incrementMessageCount = false
}) {
  if (!chatId) return;
  try {
    const db = await getDB();
    const uniqueParticipants = [...new Set((participants || []).map(String).filter(Boolean))];
    await db.query(
      `INSERT INTO chat_conversation_metadata (
         chat_id,
         participant_user_ids,
         last_message_preview,
         last_message_type,
         last_message_at,
         last_sender_user_id,
         message_count,
         source,
         interest_id,
         created_at,
         updated_at
       )
       VALUES ($1,$2::text[],$3,$4,$5,$6,$7,$8,$9,NOW(),NOW())
       ON CONFLICT (chat_id) DO UPDATE
       SET participant_user_ids = CASE
             WHEN cardinality(EXCLUDED.participant_user_ids) > 0
             THEN EXCLUDED.participant_user_ids
             ELSE chat_conversation_metadata.participant_user_ids
           END,
           last_message_preview = COALESCE(EXCLUDED.last_message_preview, chat_conversation_metadata.last_message_preview),
           last_message_type = COALESCE(EXCLUDED.last_message_type, chat_conversation_metadata.last_message_type),
           last_message_at = COALESCE(EXCLUDED.last_message_at, chat_conversation_metadata.last_message_at),
           last_sender_user_id = COALESCE(EXCLUDED.last_sender_user_id, chat_conversation_metadata.last_sender_user_id),
           message_count = chat_conversation_metadata.message_count + EXCLUDED.message_count,
           source = COALESCE(EXCLUDED.source, chat_conversation_metadata.source),
           interest_id = COALESCE(EXCLUDED.interest_id, chat_conversation_metadata.interest_id),
           updated_at = NOW()`,
      [
        chatId,
        uniqueParticipants,
        preview(lastMessage),
        lastMessage?.type || null,
        lastMessage?.sentAt || (lastMessage ? new Date() : null),
        lastMessage?.senderId || null,
        incrementMessageCount ? 1 : 0,
        source,
        interestId
      ]
    );
  } catch (err) {
    logger.warn('Chat metadata mirror failed: ' + err.message);
  }
}

async function touchConversationMetadata(conversation) {
  if (!conversation?.chatId) return;
  await upsertConversationMetadata({
    chatId: conversation.chatId,
    participants: conversation.participants || [],
    lastMessage: conversation.lastMessage || null,
    source: conversation.source || null,
    interestId: conversation.interestId || null
  });
}

module.exports = {
  touchConversationMetadata,
  upsertConversationMetadata
};
