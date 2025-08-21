# Portal API
A simple portal API aiming for simplicity that adds support for block entity based portal renderers. Still a big WIP.
### Notes
- A shared stencil framebuffer is used, so the contents of one portal occasionally override others'. It's a consequence of not being able to clear only the stencil buffer for each portal.
- Sodium is a dependency, as without it chunk sections flicker. Iris isn't currently supported.
- Remote locations aren't loaded client side, needing players to be nearby for them to be rendered.
- Except for raindrops, particles don't render far away from the player, unlike block entities or terrain.
- Similarly, entities experience the same issue.
- Block entities render perfectly fine inside the portal view, but often fail outside of it while a portal is being rendered.
- Carpet Mod bots don't experience the same issue as the rest of entities.