import { beforeEach, describe, expect, it } from 'vitest'
import { useUiStore } from '@/shared/stores/uiStore'

describe('useUiStore', () => {
  beforeEach(() => {
    useUiStore.getState().reset()
  })

  it('manages sidebar and mobile navigation state only', () => {
    useUiStore.getState().toggleSidebar()
    useUiStore.getState().toggleMobileNav()

    expect(useUiStore.getState().isSidebarCollapsed).toBe(true)
    expect(useUiStore.getState().isMobileNavOpen).toBe(true)

    useUiStore.getState().closeMobileNav()
    expect(useUiStore.getState().isMobileNavOpen).toBe(false)
  })
})
