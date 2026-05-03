const mongoose = require('mongoose');
const messageSchema = new mongoose.Schema({
  chatId: { type: String, required: true, index: true },
  senderId: { type: String, required: true },
  receiverId: { type: String, required: true },
  type: { type: String, enum: ['text','voice','photo','bot','system'], default: 'text' },
  content: { type: String, required: true },
  duration: { type: Number },
  status: { type: String, enum: ['sent','delivered','read'], default: 'sent' },
  flowStepId: { type: String, index: true },
  alias: { type: String },
  messageType: { type: String },
  messageUserType: { type: String },
  messageUserAlias: { type: String },
  createdMillis: { type: Number },
  safetyFlags: { type: Array, default: [] },
  sentAt: { type: Date, default: Date.now },
  deliveredAt: { type: Date },
  readAt: { type: Date }
}, { timestamps: true });
messageSchema.index({ chatId: 1, sentAt: -1 });
module.exports = mongoose.model('Message', messageSchema);
