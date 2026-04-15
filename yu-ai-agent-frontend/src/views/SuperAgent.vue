<template>
  <div class="page-shell">
    <main class="workspace-page">
      <header class="workspace-header super-header">
        <button type="button" class="back-link" @click="goBack">返回首页</button>
        <div>
          <p class="workspace-label">Super Agent</p>
          <h1>超级智能体</h1>
        </div>
        <div class="status-card">
          <span>当前状态</span>
          <strong>{{ statusLabel }}</strong>
        </div>
      </header>

      <ChatRoom
        :messages="messages"
        :connection-status="connectionStatus"
        ai-type="super"
        badge="任务助手"
        title="想查、想整理、想生成结果，都可以直接说"
        description="适合做攻略、查资料、列清单、整理输出。"
        placeholder="比如：请帮我查找武汉市的人气景点，规划一日游并生成 pdf"
        :suggestions="suggestions"
        :highlights="highlights"
        agent-name="超级智能体"
        @send-message="sendMessage"
      />
    </main>
    <AppFooter />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import ChatRoom from '../components/ChatRoom.vue'
import AppFooter from '../components/AppFooter.vue'
import { chatWithManus, resolveApiUrl } from '../api'

useHead({
  title: '超级智能体',
  meta: [
    {
      name: 'description',
      content: '适合处理多步骤任务的 AI 助手。'
    }
  ]
})

const PDF_READY_PREFIX = 'PDF_READY:'

const router = useRouter()
const messages = ref([])
const connectionStatus = ref('disconnected')
let eventSource = null
let receivedAiMessage = false

const suggestions = [
  '请帮我查找武汉市的人气景点，规划一日游并生成 pdf',
  '帮我整理一份适合周末自驾的杭州周边短途方案',
  '请帮我调研 3 款适合 Java 开发者的 AI 编程工具并输出比较表'
]

const highlights = [
  '适合一步步完成的任务',
  '支持搜索、写文件和生成结果',
  '过程和结论会分开显示'
]

const statusLabel = computed(() => {
  if (connectionStatus.value === 'connecting') return '正在执行'
  if (connectionStatus.value === 'error') return '执行异常'
  return '等待任务'
})

const addMessage = (payload, isUser) => {
  if (typeof payload === 'string') {
    messages.value.push({
      content: payload,
      isUser,
      time: Date.now()
    })
    return
  }
  messages.value.push({
    isUser,
    time: Date.now(),
    ...payload
  })
}

const tryParsePdfPayload = (rawPayload) => {
  const normalizedPayload = rawPayload.trim()
  const candidates = [
    normalizedPayload,
    normalizedPayload.replace(/\\"/g, '"'),
    normalizedPayload.replace(/^"|"$/g, '').replace(/\\"/g, '"')
  ]

  for (const candidate of candidates) {
    try {
      return JSON.parse(candidate)
    } catch {
      // 继续尝试下一种格式
    }
  }
  return null
}

const parseAssistantMessage = (rawMessage) => {
  if (!rawMessage.startsWith(PDF_READY_PREFIX)) {
    return { content: rawMessage }
  }
  const payload = tryParsePdfPayload(rawMessage.slice(PDF_READY_PREFIX.length))
  if (!payload?.fileName || !payload?.downloadPath) {
    return { content: rawMessage }
  }
  return {
    content: `PDF 已生成：${payload.fileName}`,
    downloadUrl: resolveApiUrl(payload.downloadPath),
    fileName: payload.fileName
  }
}

const resetStreamState = () => {
  receivedAiMessage = false
}

const closeCurrentStream = () => {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
}

const finishStream = () => {
  connectionStatus.value = 'disconnected'
  closeCurrentStream()
}

const sendMessage = (message) => {
  addMessage(message, true)
  closeCurrentStream()
  resetStreamState()

  connectionStatus.value = 'connecting'
  eventSource = chatWithManus(message)

  eventSource.onmessage = (event) => {
    const data = event.data?.trim()
    if (!data || data === '[DONE]') {
      finishStream()
      return
    }
    receivedAiMessage = true
    addMessage(parseAssistantMessage(data), false)
  }

  eventSource.onerror = () => {
    if (receivedAiMessage) {
      finishStream()
      return
    }
    connectionStatus.value = 'error'
    closeCurrentStream()
  }
}

const goBack = () => router.push('/')

onMounted(() => {
  addMessage('你好，我是超级智能体。把目标说清楚一点，我会帮你一步步处理。', false)
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
  grid-template-columns: 150px minmax(0, 1fr) 210px;
  gap: 14px;
  align-items: center;
  margin-bottom: 16px;
  padding: 22px 24px;
  border-radius: 28px;
  box-shadow: var(--shadow);
}

.super-header {
  background: linear-gradient(135deg, rgba(236, 248, 245, 0.95), rgba(232, 242, 255, 0.88));
  border: 1px solid rgba(15, 118, 110, 0.14);
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
  color: var(--primary-deep);
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

.status-card {
  justify-self: end;
  padding: 12px 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.76);
  display: grid;
  gap: 4px;
}

.status-card span {
  color: var(--text-muted);
  font-size: 0.78rem;
}

.status-card strong {
  font-size: 0.95rem;
}

@media (max-width: 1024px) {
  .workspace-header {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .status-card,
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