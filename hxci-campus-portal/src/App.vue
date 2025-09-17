<template>
  <div id="app">
    <RouterView />
  </div>
</template>

<script setup lang="ts">
// 这里可以添加全局逻辑
</script>

<style scoped>
#app {
  width: 100%;
  height: 100vh;
}
</style>

<!-- 全局导入CSS变量，不使用scoped -->
<style lang="scss">
@import '@/styles/variables.scss';
</style>