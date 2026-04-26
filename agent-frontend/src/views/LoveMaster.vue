<template>
  <div class="page-shell">
    <main class="workspace-page">
      <header class="workspace-header love-header">
        <button type="button" class="back-link" @click="goBack">返回首页</button>
        <div>
          <p class="workspace-label">Love Master</p>
          <h1>恋爱大师</h1>
        </div>
        <div class="session-card">
          <span>会话 ID</span>
          <strong>{{ chatId }}</strong>
        </div>
      </header>

      <ChatRoom
        :messages="messages"
        :connection-status="connectionStatus"
        ai-type="love"
        badge="陪伴助手"
        title="把你的情况告诉我，我们慢慢聊"
        description="适合聊关系、沟通、情绪和相处方式。"
        placeholder="比如：我们总因为沟通方式吵架，我该怎么说会更好？"
        :suggestions="suggestions"
        :highlights="highlights"
        agent-name="恋爱大师"
        @send-message="sendMessage"
      />
    </main>
    <AppFooter />
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import ChatRoom from '../components/ChatRoom.vue'
import AppFooter from '../components/AppFooter.vue'
import { chatWithLoveApp } from '../api'

useHead({
  title: '恋爱大师',
  meta: [
    {
      name: 'description',
      content: '适合情感和关系话题的 AI 助手。'
    }
  ]
})

const router = useRouter()
const messages = ref([])
const chatId = ref('')
const connectionStatus = ref('disconnected')
let eventSource = null

const suggestions = [
  '我总是不知道怎么表达在意，能不能教我几种自然的说法？',
  '异地恋越来越累，我该怎么判断是坚持还是调整相处方式？',
  '对方回消息很慢，我总会多想，怎么让自己稳定一点？'
]

const highlights = [
  '适合聊关系和沟通问题',
  '支持连续对话',
  '更偏陪伴和梳理感受'
]

const generateChatId = () => `love_${Math.random().toString(36).slice(2, 10)}`

const addMessage = (content, isUser) => {
  messages.value.push({
    content,
    isUser,
    time: Date.now()
  })
}

const closeCurrentStream = () => {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
}

const sendMessage = (message) => {
  addMessage(message, true)
  closeCurrentStream()

  let aiMessageIndex = -1
  connectionStatus.value = 'connecting'
  eventSource = chatWithLoveApp(message, chatId.value)

  eventSource.onmessage = (event) => {
    const data = event.data
    if (data === '[DONE]') {
      connectionStatus.value = 'disconnected'
      closeCurrentStream()
      return
    }

    if (aiMessageIndex === -1) {
      addMessage('', false)
      aiMessageIndex = messages.value.length - 1
    }

    if (aiMessageIndex >= 0) {
      messages.value[aiMessageIndex].content += data
    }
  }

  eventSource.onerror = () => {
    connectionStatus.value = 'error'
    closeCurrentStream()
  }
}

const goBack = () => router.push('/')

onMounted(() => {
  chatId.value = generateChatId()
  addMessage('你好，我是恋爱大师。把你的情况告诉我，我们一步一步聊。', false)
})

onBeforeUnmount(closeCurrentStream)
</script>

<style scoped>
.page-shell {
  min-height: 100vh;
  padding-top: 24px;
}

.workspace-page {
  width: min(calc(100% - 32px), var(--container));
  margin: 0 auto;
}

.workspace-header {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr) 230px;
  gap: 14px;
  align-items: center;
  margin-bottom: 16px;
  padding: 22px 24px;
  border-radius: 28px;
  box-shadow: var(--shadow);
}

.love-header {
  background: linear-gradient(135deg, rgba(255, 248, 244, 0.95), rgba(255, 239, 232, 0.88));
  border: 1px solid rgba(209, 77, 114, 0.14);
}

.back-link {
  justify-self: start;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(31, 42, 36, 0.08);
  font-size: 0.9rem;
}

.workspace-label {
  margin: 0 0 6px;
  color: var(--accent);
  font-size: 0.76rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-weight: 700;
}

.workspace-header h1 {
  margin: 0;
  font-size: clamp(1.7rem, 2.4vw, 2.4rem);
  line-height: 1;
}

.session-card {
  justify-self: end;
  min-width: 0;
  padding: 12px 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.76);
  display: grid;
  gap: 4px;
}

.session-card span {
  color: var(--text-muted);
  font-size: 0.78rem;
}

.session-card strong {
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 0.92rem;
}

@media (max-width: 1024px) {
  .workspace-header {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .session-card,
  .back-link {
    justify-self: start;
  }
}

@media (max-width: 640px) {
  .page-shell {
    padding-top: 16px;
  }

  .workspace-page {
    width: min(calc(100% - 20px), var(--container));
  }

  .workspace-header {
    padding: 18px;
    border-radius: 22px;
  }
}
</style>