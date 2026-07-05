import {createRouter, createWebHistory} from 'vue-router'
import Config from '../pages/Config.vue'
import Home from '../pages/Home.vue'
import OAuthCallback from '../pages/OAuthCallback.vue'
import Me from '../pages/Me.vue'
import {hasOAuth2Config} from '../utils/configStorage'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {path: '/', name: 'config', component: Config},
    {path: '/home', name: 'home', component: Home},
    {path: '/oauth/callback', name: 'oauth-callback', component: OAuthCallback},
    {path: '/me', name: 'me', component: Me}
  ]
})

router.beforeEach((to) => {
  if (to.name !== 'config' && !hasOAuth2Config()) {
    return {name: 'config'}
  }
  return true
})

export default router

