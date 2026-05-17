const Message = require('../models/Message');
const ArchivedMessage = require('../models/ArchivedMessage');
const logger = require('../utils/logger');

const retentionMonths = () => Math.max(parseInt(process.env.CHAT_RETENTION_MONTHS || '24', 10), 1);
const archiveBatchSize = () => Math.min(Math.max(parseInt(process.env.CHAT_ARCHIVE_BATCH_SIZE || '500', 10), 10), 2000);
const shouldDeleteSource = () => process.env.CHAT_ARCHIVE_DELETE_SOURCE === 'true';

async function archiveOldMessages({ now = new Date() } = {}) {
  const cutoff = new Date(now);
  cutoff.setMonth(cutoff.getMonth() - retentionMonths());
  const messages = await Message.find({ sentAt: { $lt: cutoff } })
    .sort({ sentAt: 1 })
    .limit(archiveBatchSize())
    .lean();
  if (!messages.length) {
    return { archived: 0, deleted: 0, cutoff };
  }
  const operations = messages.map((message) => ({
    updateOne: {
      filter: { originalMessageId: message._id.toString() },
      update: {
        $setOnInsert: {
          originalMessageId: message._id.toString(),
          chatId: message.chatId,
          senderId: message.senderId,
          receiverId: message.receiverId,
          type: message.type,
          content: message.content,
          duration: message.duration,
          status: message.status,
          safetyFlags: message.safetyFlags || [],
          sentAt: message.sentAt,
          archivedAt: new Date()
        }
      },
      upsert: true
    }
  }));
  await ArchivedMessage.bulkWrite(operations, { ordered: false });
  let deleted = 0;
  if (shouldDeleteSource()) {
    const ids = messages.map((message) => message._id);
    const result = await Message.deleteMany({ _id: { $in: ids } });
    deleted = result.deletedCount || 0;
  }
  return { archived: messages.length, deleted, cutoff };
}

function startChatArchiveJob() {
  if (process.env.CHAT_ARCHIVE_JOB_DISABLED === 'true') {
    logger.info('Chat archive job disabled by CHAT_ARCHIVE_JOB_DISABLED=true');
    return null;
  }
  const intervalMs = Math.max(parseInt(process.env.CHAT_ARCHIVE_INTERVAL_MS || String(24 * 60 * 60 * 1000), 10), 60 * 60 * 1000);
  const run = () => archiveOldMessages()
    .then((result) => {
      if (result.archived) logger.info(`Archived ${result.archived} chat messages; deleted ${result.deleted}`);
    })
    .catch((err) => logger.warn('Chat archive job failed: ' + err.message));
  const timer = setInterval(run, intervalMs);
  timer.unref?.();
  setTimeout(run, 30 * 1000).unref?.();
  return timer;
}

module.exports = {
  _test: { retentionMonths, shouldDeleteSource },
  archiveOldMessages,
  startChatArchiveJob
};
