const mongoose = require('mongoose');
const conversationSchema = new mongoose.Schema({
  chatId: { type: String, required: true, unique: true },
  participants: [{ type: String }],
  lastMessage: { content: String, type: String, sentAt: Date, senderId: String },
  unreadCounts: { type: Map, of: Number, default: {} },
  flowId: { type: String },
  flowVersionId: { type: String },
  flowMessages: { type: Array, default: [] },
  flowMessagesJson: { type: String },
  flowBusinessHourType: { type: String },
  serviceAccountId: { type: String },
  operatingHoursId: { type: String }
}, { timestamps: true });
conversationSchema.index({ participants: 1 });
module.exports = mongoose.model('Conversation', conversationSchema);
