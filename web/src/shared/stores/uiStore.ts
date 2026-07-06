import { create } from 'zustand'

type UiState = {
  isSidebarCollapsed: boolean
  isMobileNavOpen: boolean
  toggleSidebar: () => void
  toggleMobileNav: () => void
  closeMobileNav: () => void
  reset: () => void
}

const initialState = {
  isSidebarCollapsed: false,
  isMobileNavOpen: false,
}

export const useUiStore = create<UiState>((set) => ({
  ...initialState,
  toggleSidebar: () =>
    set((state) => ({ isSidebarCollapsed: !state.isSidebarCollapsed })),
  toggleMobileNav: () =>
    set((state) => ({ isMobileNavOpen: !state.isMobileNavOpen })),
  closeMobileNav: () => set({ isMobileNavOpen: false }),
  reset: () => set(initialState),
}))
