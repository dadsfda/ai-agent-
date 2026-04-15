<template>
  <div class="avatar-shell" :class="type">
    <span v-if="type === 'love'">爱</span>
    <span v-else>智</span>
  </div>
</template>

<script setup>
defineProps({
  type: {
    type: String,
    default: 'super'
  }
})
</script>

<style scoped>
.avatar-shell {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: white;
  font-size: 0.95rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.love {
  background: linear-gradient(135deg, #d14d72, #f08b66);
}

.super,
.default {
  background: linear-gradient(135deg, #0f766e, #1d4ed8);
}
</style>