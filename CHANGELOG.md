## Release

### [1.4.2] - 2026-01-11

#### Fixed
- Fixed NullPointerException when comparing ItemStacks in shulker refill handler
- Fixed NullPointerException when itemToRefill is null
- Improved null safety by storing complete ItemStack copy instead of just the Item reference
- Enhanced stability of automatic refill system from shulker boxes
