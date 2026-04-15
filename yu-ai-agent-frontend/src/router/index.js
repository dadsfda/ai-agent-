import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: {
      title: 'AI Agent 工作台',
      description: '基于 Spring AI 的双智能体应用，包含恋爱大师与超级智能体工作台。'
    }
  },
  {
    path: '/love-master',
    name: 'LoveMaster',
    component: () => import('../views/LoveMaster.vue'),
    meta: {
      title: '恋爱大师工作台',
      description: '面向情感咨询场景的 AI 对话工作台。'
    }
  },
  {
    path: '/super-agent',
    name: 'SuperAgent',
    component: () => import('../views/SuperAgent.vue'),
    meta: {
      title: '超级智能体工作台',
      description: '支持工具调用、分步执行与结果产出的智能体工作台。'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = to.meta.title
  }
  next()
})

export default router