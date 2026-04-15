<template>
  <div class="page-shell">
    <main class="home-page">
      <section class="hero-card">
        <div class="hero-copy">
          <p class="eyebrow">AI Agent</p>
          <h1>两种好用的 AI 助手，按你的场景直接选</h1>
          <p class="hero-desc">
            想聊情感、关系和沟通，就用恋爱大师；想查资料、做规划、生成文件，就用超级智能体。
          </p>
          <div class="hero-actions">
            <button type="button" class="primary-btn" @click="navigateTo('/super-agent')">打开超级智能体</button>
            <button type="button" class="secondary-btn" @click="navigateTo('/love-master')">打开恋爱大师</button>
          </div>
        </div>
        <div class="hero-panel">
          <div class="hero-stat">
            <span>2</span>
            <small>可直接使用的助手</small>
          </div>
          <div class="hero-stat">
            <span>多步骤</span>
            <small>支持过程可视化</small>
          </div>
          <div class="hero-note">
            直接提需求就可以开始，不用先了解技术细节。
          </div>
        </div>
      </section>

      <section class="card-grid">
        <article class="product-card love" @click="navigateTo('/love-master')">
          <div class="card-top">
            <span class="card-icon">爱</span>
            <span class="card-tag">陪伴对话</span>
          </div>
          <h2>恋爱大师</h2>
          <p>适合聊感情、关系、沟通方式，也适合想找个人一起理清情绪的时候。</p>
          <ul>
            <li>更自然的聊天体验</li>
            <li>支持连续对话</li>
            <li>适合慢慢聊清楚问题</li>
          </ul>
          <div class="card-link">进入聊天</div>
        </article>

        <article class="product-card super" @click="navigateTo('/super-agent')">
          <div class="card-top">
            <span class="card-icon">智</span>
            <span class="card-tag">任务处理</span>
          </div>
          <h2>超级智能体</h2>
          <p>适合查资料、做计划、整理结果，或者把一个任务拆成几步慢慢完成。</p>
          <ul>
            <li>会分步骤执行任务</li>
            <li>支持搜索和文件生成</li>
            <li>结果会单独展示出来</li>
          </ul>
          <div class="card-link">立即开始</div>
        </article>
      </section>

      <section class="feature-strip">
        <div class="feature-item">
          <strong>上手简单</strong>
          <span>打开就能用，适合先试一试再深入。</span>
        </div>
        <div class="feature-item">
          <strong>过程清楚</strong>
          <span>重要步骤会展示出来，方便你判断结果是否靠谱。</span>
        </div>
        <div class="feature-item">
          <strong>手机也能用</strong>
          <span>页面做了收口，日常浏览和输入会更轻松。</span>
        </div>
      </section>
    </main>
    <AppFooter />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import AppFooter from '../components/AppFooter.vue'

useHead({
  title: 'AI 助手工作台',
  meta: [
    {
      name: 'description',
      content: '两个可直接体验的 AI 助手入口。'
    }
  ]
})

const router = useRouter()
const navigateTo = (path) => router.push(path)
</script>

<style scoped>
.page-shell {
  min-height: 100vh;
  padding: 24px 0 0;
}

.home-page {
  width: min(calc(100% - 32px), var(--container));
  margin: 0 auto;
}

.hero-card {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(260px, 0.8fr);
  gap: 20px;
  padding: 30px;
  border-radius: 34px;
  background: linear-gradient(135deg, rgba(255, 252, 247, 0.95), rgba(249, 244, 234, 0.9));
  border: 1px solid rgba(31, 42, 36, 0.08);
  box-shadow: var(--shadow);
}

.eyebrow {
  margin: 0 0 10px;
  color: var(--primary-deep);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 0;
  font-size: clamp(2rem, 3.3vw, 3.4rem);
  line-height: 1.02;
  letter-spacing: -0.03em;
}

.hero-desc {
  max-width: 620px;
  margin: 16px 0 0;
  color: var(--text-muted);
  font-size: 0.98rem;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  flex-wrap: wrap;
}

.primary-btn,
.secondary-btn {
  padding: 12px 18px;
  border-radius: 999px;
  font-weight: 700;
  font-size: 0.95rem;
}

.primary-btn {
  background: linear-gradient(135deg, var(--primary), var(--primary-deep));
  color: white;
}

.secondary-btn {
  background: rgba(255, 255, 255, 0.9);
  color: var(--text);
  border: 1px solid var(--line);
}

.hero-panel {
  border-radius: 24px;
  padding: 22px;
  background: linear-gradient(180deg, #21413b, #172924);
  color: var(--text-light);
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  align-content: start;
}

.hero-stat {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.06);
}

.hero-stat span {
  display: block;
  font-size: 1.7rem;
  font-weight: 700;
}

.hero-stat small {
  color: rgba(238, 243, 239, 0.74);
  font-size: 0.85rem;
}

.hero-note {
  grid-column: 1 / -1;
  padding: 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(209, 77, 114, 0.22), rgba(217, 119, 6, 0.22));
  line-height: 1.6;
  font-size: 0.92rem;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
  margin-top: 22px;
}

.product-card {
  padding: 24px;
  border-radius: 28px;
  background: rgba(255, 252, 247, 0.88);
  border: 1px solid rgba(31, 42, 36, 0.08);
  box-shadow: var(--shadow);
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.product-card:hover {
  transform: translateY(-4px);
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-icon {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
  font-size: 0.95rem;
}

.product-card.love .card-icon {
  background: linear-gradient(135deg, #d14d72, #f08b66);
}

.product-card.super .card-icon {
  background: linear-gradient(135deg, #0f766e, #2563eb);
}

.card-tag {
  padding: 6px 11px;
  border-radius: 999px;
  background: rgba(31, 42, 36, 0.06);
  font-size: 0.78rem;
  color: var(--text-muted);
}

.product-card h2 {
  margin: 16px 0 8px;
  font-size: 1.55rem;
}

.product-card p {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.94rem;
}

.product-card ul {
  margin: 18px 0 0;
  padding-left: 18px;
  color: var(--text-muted);
  line-height: 1.75;
  font-size: 0.91rem;
}

.card-link {
  margin-top: 18px;
  color: var(--primary-deep);
  font-weight: 700;
  font-size: 0.94rem;
}

.feature-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin: 22px 0;
}

.feature-item {
  padding: 18px 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(31, 42, 36, 0.08);
}

.feature-item strong {
  display: block;
  margin-bottom: 6px;
  font-size: 0.95rem;
}

.feature-item span {
  color: var(--text-muted);
  font-size: 0.9rem;
}

@media (max-width: 960px) {
  .hero-card,
  .card-grid,
  .feature-strip {
    grid-template-columns: 1fr;
  }

  .hero-card {
    padding: 24px;
  }
}

@media (max-width: 640px) {
  .page-shell {
    padding-top: 16px;
  }

  .home-page {
    width: min(calc(100% - 20px), var(--container));
  }

  .hero-card,
  .product-card,
  .feature-item {
    border-radius: 24px;
  }

  .hero-copy h1 {
    font-size: 2rem;
  }
}
</style>