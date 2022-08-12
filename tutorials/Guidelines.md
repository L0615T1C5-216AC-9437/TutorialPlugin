# WIP
> **The guidelines for tutorial code is still work in progress!**
### Layout
* Last code line *must* be an always jump to itself, to prevent the tutorial from re-executing
* The tutorial must always end up executing the last line of code, no end blocks allowed.
### Unit Spawn
* When spawning a unit, the unit's flag must be set to (@thisX * 10000 + @thisY) so the server can kill them once the tutorial ends.  
* Units must not leave the 20x20 area top right of the proc.  
* Units must not disturb others outside the 20x20 area top right of proc
### Set Rule
Not allowed
