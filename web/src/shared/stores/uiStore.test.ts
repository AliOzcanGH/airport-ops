import { beforeEach, describe, expect, it } from 'vitest'
import { useUiStore } from '@/shared/stores/uiStore'

describe('useUiStore', () => {
  it('contains only client-side navigation state', () => {
    expect(Object.keys(useUiStore.getState()).sort()).toEqual([
      'closeMobileNav',
      'isMobileNavOpen',
      'isSidebarCollapsed',
      'reset',
      'toggleMobileNav',
      'toggleSidebar',
    ])
  })

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
