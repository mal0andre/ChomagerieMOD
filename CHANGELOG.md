# Changelog

All notable changes to this project will be documented in this file.

## [1.3]

### Added
- Customizable refill messages with `%s` placeholder for item name
- Item filtering system: configure which items can be refilled (empty list = all items allowed)
- New command `/chomagerie shulkerrefill message <message>` to customize refill message
- New commands for item management:
  - `/chomagerie shulkerrefill items add <itemId>` - Add an item to the allowed list
  - `/chomagerie shulkerrefill items remove <itemId>` - Remove an item from the list
  - `/chomagerie shulkerrefill items list` - Show all allowed items
  - `/chomagerie shulkerrefill items clear` - Clear the list (allow all items)
- Server-side validation to prevent refilling of non-allowed items

### Fixed
- None