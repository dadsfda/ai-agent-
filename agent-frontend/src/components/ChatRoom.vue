<template>
  <section class="workspace-shell">
    <div class="workspace-intro">
      <div class="workspace-badge">{{ badge }}</div>
      <h2 class="workspace-title">{{ title }}</h2>
      <p class="workspace-desc">{{ description }}</p>
      <ul v-if="highlights.length" class="workspace-list">
        <li v-for="item in highlights" :key="item">{{ item }}</li>
      </ul>
      <div v-if="suggestions.length" class="suggestions">
        <button
          v-for="item in suggestions"
          :key="item"
          class="suggestion-chip"
          type="button"
          :disabled="connectionStatus === 'connecting'"
          @click="quickSend(item)"
        >
          {{ item }}
        </button>
      </div>
    </div>

    <div class="chat-panel">
      <div ref="messagesContainer" class="chat-messages">
        <div v-for="(msg, index) in messages" :key="`${msg.time}-${index}`" class="message-row" :class="msg.isUser ? 'is-user' : 'is-ai'">
          <div v-if="!msg.isUser" class="avatar ai-avatar">
            <AiAvatarFallback :type="aiType" />
          </div>
          <article class="message-card" :class="msg.isUser ? 'user-card' : 'ai-card'">
            <div class="message-meta">
              <span>{{ msg.isUser ? '你' : agentName }}</span>
              <span>{{ formatTime(msg.time) }}</span>
            </div>
            <div class="message-content">{{ msg.content }}<span v-if="showTyping(index)" class="typing-dot">●</span></div>
            <a
              v-if="msg.downloadUrl"
              class="download-link"
              :href="msg.downloadUrl"
              :download="msg.fileName || true"
              target="_blank"
              rel="noreferrer"
            >
              下载 PDF
            </a>
          </article>
          <div v-if="msg.isUser" class="avatar user-avatar">你</div>
        </div>
      </div>

      <div class="composer">
        <div class="composer-status">
          <span class="status-dot" :class="connectionStatus"></span>
          <span>{{ statusText }}</span>
        </div>
        <textarea
          v-model="inputMessage"
          class="composer-input"
          :placeholder="placeholder"
          :disabled="connectionStatus === 'connecting'"
          @keydown.enter.exact.prevent="sendMessage"
        ></textarea>
        <div class="composer-actions">
          <span class="composer-tip">Enter 发送，Shift + Enter 换行</span>
          <button
            class="send-button"
            type="button"
            :disabled="connectionStatus === 'connecting' || !inputMessage.trim()"
            @click="sendMessage"
          >
            {{ connectionStatus === 'connecting' ? '处理中...' : '发送' }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import AiAvatarFallback from './AiAvatarFallback.vue'

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  connectionStatus: {
    type: String,
    default: 'disconnected'
  },
  aiType: {
    type: String,
    default: 'super'
  },
  badge: {
    type: String,
    default: '助手'
  },
  title: {
    type: String,
    default: '和助手聊聊'
  },
  description: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '输入你想问的问题...'
  },
  suggestions: {
    type: Array,
    default: () => []
  },
  highlights: {
    type: Array,
    default: () => []
  },
  agentName: {
    type: String,
    default: 'AI 助手'
  }
})

const emit = defineEmits(['send-message'])

const inputMessage = ref('')
const messagesContainer = ref(null)

const statusText = computed(() => {
  if (props.connectionStatus === 'connecting') return '正在处理'
  if (props.connectionStatus === 'error') return '连接出现问题'
  return '等待输入'
})

const sendMessage = () => {
  const message = inputMessage.value.trim()
  if (!message) {
    return
  }
  emit('send-message', message)
  inputMessage.value = ''
}

const quickSend = (message) => {
  emit('send-message', message)
}

const formatTime = (timestamp) => {
  return new Date(timestamp).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

const showTyping = (index) => {
  return props.connectionStatus === 'connecting' && index === props.messages.length - 1 && !props.messages[index].isUser
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

watch(() => props.messages.map((item) => item.content).join('\n'), scrollToBottom)
watch(() => props.messages.length, scrollToBottom)

onMounted(scrollToBottom)
</script>

<style scoped>
.workspace-shell {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 18px;
  min-height: 700px;
}

.workspace-intro,
.chat-panel {
  border: 1px solid var(--line);
  background: var(--surface);
  backdrop-filter: blur(14px);
  box-shadow: var(--shadow);
}

.workspace-intro {
  border-radius: var(--radius-xl);
  padding: 24px;
  align-self: stretch;
}

.workspace-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 11px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.1);
  color: var(--primary-deep);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.workspace-title {
  margin: 14px 0 8px;
  font-size: 1.55rem;
  line-height: 1.12;
}

.workspace-desc {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.92rem;
}

.workspace-list {
  margin: 18px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}

.workspace-list li {
  padding: 11px 13px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(31, 42, 36, 0.08);
  color: var(--text-muted);
  font-size: 0.88rem;
}

.suggestions {
  margin-top: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
}

.suggestion-chip {
  padding: 9px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(31, 42, 36, 0.1);
  color: var(--text);
  font-size: 0.86rem;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.suggestion-chip:hover:not(:disabled) {
  transform: translateY(-2px);
  border-color: rgba(15, 118, 110, 0.4);
  box-shadow: 0 12px 24px rgba(15, 118, 110, 0.12);
}

.chat-panel {
  border-radius: var(--radius-xl);
  padding: 16px;
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  min-height: 700px;
}

.chat-messages {
  min-height: 0;
  overflow-y: auto;
  padding: 8px 4px 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.message-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.message-row.is-user {
  justify-content: flex-end;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-avatar {
  background: var(--surface-dark);
  color: var(--text-light);
  font-size: 0.82rem;
  font-weight: 700;
}

.message-card {
  max-width: min(78%, 680px);
  padding: 12px 14px;
  border-radius: 18px;
}

.ai-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(31, 42, 36, 0.08);
}

.user-card {
  background: linear-gradient(135deg, var(--primary), #0b5d57);
  color: white;
}

.message-meta {
  display: flex;
  gap: 10px;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 0.72rem;
  opacity: 0.72;
}

.message-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.65;
  font-size: 0.93rem;
}

.download-link {
  display: inline-flex;
  margin-top: 10px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.12);
  color: var(--primary-deep);
  font-size: 0.86rem;
  font-weight: 700;
}

.typing-dot {
  margin-left: 2px;
  animation: blink 0.9s infinite;
}

.composer {
  border-top: 1px solid var(--line);
  padding-top: 14px;
}

.composer-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 0.82rem;
  margin-bottom: 10px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #94a3b8;
}

.status-dot.connecting {
  background: var(--secondary);
  box-shadow: 0 0 0 6px rgba(217, 119, 6, 0.16);
}

.status-dot.error {
  background: #dc2626;
}

.status-dot.disconnected {
  background: var(--primary);
}

.composer-input {
  width: 100%;
  min-height: 108px;
  resize: vertical;
  border: 1px solid var(--line-strong);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  padding: 14px 16px;
  color: var(--text);
  outline: none;
  font-size: 0.93rem;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.composer-input:focus {
  border-color: rgba(15, 118, 110, 0.45);
  box-shadow: 0 0 0 4px rgba(15, 118, 110, 0.08);
}

.composer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
}

.composer-tip {
  color: var(--text-muted);
  font-size: 0.8rem;
}

.send-button {
  padding: 11px 16px;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--primary), var(--primary-deep));
  color: white;
  font-weight: 700;
  font-size: 0.9rem;
}

.send-button:disabled,
.suggestion-chip:disabled,
.composer-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@keyframes blink {
  0%,
  100% { opacity: 0.2; }
  50% { opacity: 1; }
}

@media (max-width: 1024px) {
  .workspace-shell {
    grid-template-columns: 1fr;
  }

  .workspace-intro {
    padding: 22px;
  }
}

@media (max-width: 768px) {
  .chat-panel,
  .workspace-intro {
    border-radius: 22px;
  }

  .chat-panel {
    min-height: 600px;
    padding: 14px;
  }

  .message-card {
    max-width: 86%;
  }

  .composer-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .send-button {
    width: 100%;
  }
}
</style>