const mongoose = require('mongoose');

const archivedMessageSchema = new mongoose.Schema({
  originalMessageId: { type: String, required: true, unique: true, index: true },
  chatId: { type: String, required: true, index: true },
  senderId: { type: String, required: true },
  receiverId: { type: String, required: true },
  type: { type: String },
  content: { type: String },
  duration: { type: Number },
  status: { type: String },
  safetyFlags: { type: Array, default: [] },
  sentAt: { type: Date, index: true },
  archivedAt: { type: Date, default: Date.now, index: true }
}, { timestamps: true });

module.exports = mongoose.model('ArchivedMessage', archivedMessageSchema);
