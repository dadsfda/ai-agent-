<template>
  <footer class="footer">
    <div class="footer-inner">
      <div>
        <div class="brand">AI 助手工作台</div>
        <p class="copy">打开页面就能试，用起来再慢慢熟悉也不迟。</p>
      </div>
      <div class="links">
        <a href="https://github.com/dadsfda/ai-agent-" target="_blank" rel="noreferrer">GitHub</a>
        <a href="http://localhost:8123/api/swagger-ui.html" target="_blank" rel="noreferrer">Swagger</a>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  padding: 20px 0 28px;
}

.footer-inner {
  width: min(calc(100% - 32px), var(--container));
  margin: 0 auto;
  padding: 20px 22px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: rgba(255, 252, 247, 0.7);
  backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand {
  font-size: 0.94rem;
  font-weight: 700;
}

.copy {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 0.86rem;
}

.links {
  display: flex;
  gap: 16px;
  color: var(--primary-deep);
  font-weight: 600;
  font-size: 0.9rem;
}

@media (max-width: 768px) {
  .footer-inner {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>