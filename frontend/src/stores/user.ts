import { defineStore } from 'pinia';

export const useUserStore = defineStore('user', {
  state: (): { token: string } => ({
    token: ''
  }),
  actions: {
    setToken(token: string) {
      (this as any).token = token;
    }
  }
}); 