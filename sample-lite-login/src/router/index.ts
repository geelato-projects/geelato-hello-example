import {createRouter, createWebHistory} from 'vue-router'
import Config from '../pages/Config.vue'
import Demo from '../pages/Demo.vue'
import {hasLiteLoginConfig} from '../utils/configStorage'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {path: '/', name: 'config', component: Config},
    {path: '/demo', name: 'demo', component: Demo}
  ]
})

router.beforeEach((to) => {
  if (to.name !== 'config' && !hasLiteLoginConfig()) {
    return {name: 'config'}
  }
  return true
})

export default router

