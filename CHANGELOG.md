## Release

### [1.4.4] - 2026-01-21

#### Fixed
- Fixed bucket refill from shulker boxes not working when bucket is emptied
- Added support for all bucket types: water, lava, powder snow, milk, and mob buckets (fish, axolotl, tadpole)
- The mod now correctly detects when a filled bucket becomes empty and triggers the refill mechanism
- **Fixed empty buckets disappearing after refill**: Empty buckets are now properly saved to the player's inventory instead of being lost
- If the player's inventory is full, empty buckets will be dropped on the ground instead of vanishing

#### How it works
- When you use a filled bucket (e.g., water bucket), it becomes an empty bucket
- The mod detects this change and automatically refills with a new filled bucket from your shulker box
- **The empty bucket is saved to your inventory** (it will try to stack with existing empty buckets)
- This allows continuous use: filled bucket → use → auto-refill → empty bucket saved to inventory
