-- LYRA-8 Debug Script
-- Simple script to check file structure

print("=== LYRA-8 DEBUG ===")
print("Script path:", debug.getinfo(1).source)

-- Try to list directory contents
print("\n=== CHECKING FILE STRUCTURE ===")

-- Check if files exist
local function file_exists(path)
    local file = io.open(path, "r")
    if file then
        file:close()
        return true
    else
        return false
    end
end

-- Check current directory
print("Current working directory contents:")
os.execute("ls -la")

print("\nChecking for lib directory:")
os.execute("ls -la lib/")

print("\nChecking for lib/lyra8 directory:")
os.execute("ls -la lib/lyra8/")

print("\nLua require paths:")
print("package.path:", package.path)

-- Try to find any .lua files
print("\nLooking for .lua files:")
os.execute("find . -name '*.lua' -type f")

print("\n=== DEBUG COMPLETE ===")
