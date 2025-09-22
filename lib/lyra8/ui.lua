-- LYRA-8 UI Module
-- Handles screen drawing and visual feedback

local UI = {}

-- Visual constants
local FONT_SIZE = 8
local LINE_HEIGHT = 10
local SCREEN_W = 128
local SCREEN_H = 64

-- Animation constants
local BLINK_RATE = 15  -- frames per blink cycle

function UI.init()
    screen.clear()
    screen.update()
end

-- Main drawing function
function UI.draw(app)
    screen.clear()
    
    -- Draw title bar
    UI.draw_title_bar(app)
    
    -- Draw current level and navigation
    UI.draw_level_info(app)
    
    -- Draw parameter list for current level
    UI.draw_parameter_list(app)
    
    -- Draw voice activity visualization
    UI.draw_voice_activity(app)
    
    -- Draw status indicators
    UI.draw_status(app)
    
    screen.update()
end

function UI.draw_title_bar(app)
    screen.level(15)
    screen.move(2, 8)
    screen.text("LYRA-8")
    
    -- Show current voice if in voice mode
    if app.current_level == 4 then
        screen.move(50, 8)
        screen.text("Voice " .. app.selected_voice)
    end
    
    -- Show edit mode indicator
    if app.edit_mode then
        local blink = math.floor(app.blink_timer / 10) % 2 == 0
        if blink then
            screen.level(15)
            screen.move(SCREEN_W - 25, 8)
            screen.text("EDIT")
        end
    end
    
    -- Draw separator line
    screen.level(4)
    screen.move(0, 12)
    screen.line(SCREEN_W, 12)
    screen.stroke()
end

function UI.draw_level_info(app)
    local level_names = {"Global", "Quads", "Pairs", "Voices"}
    
    screen.level(8)
    screen.move(2, 22)
    screen.text("Level: " .. level_names[app.current_level])
    
    -- Draw level navigation indicators
    for i = 1, 4 do
        local x = 70 + (i - 1) * 12
        local y = 22
        
        if i == app.current_level then
            screen.level(15)
            screen.rect(x - 2, y - 6, 8, 8)
            screen.fill()
        else
            screen.level(4)
            screen.rect(x - 2, y - 6, 8, 8)
            screen.stroke()
        end
        
        screen.level(i == app.current_level and 0 or 8)
        screen.move(x, y)
        screen.text(tostring(i))
    end
end

function UI.draw_parameter_list(app)
    local level_params = get_current_level_params()
    if not level_params then return end
    
    local start_y = 32
    local visible_params = 3  -- Show 3 parameters at once
    
    -- Calculate scroll offset to keep selected param visible
    local scroll_offset = 0
    if app.selected_param > visible_params then
        scroll_offset = app.selected_param - visible_params
    end
    
    for i = 1, visible_params do
        local param_index = i + scroll_offset
        if param_index <= #level_params then
            local param_name = level_params[param_index]
            local param_id = get_current_param_id_for_param(param_name, app)
            local y = start_y + (i - 1) * LINE_HEIGHT
            
            -- Highlight selected parameter
            if param_index == app.selected_param then
                screen.level(8)
                screen.rect(0, y - 6, SCREEN_W, LINE_HEIGHT)
                screen.fill()
                screen.level(15)
            else
                screen.level(8)
            end
            
            -- Draw parameter name
            screen.move(4, y)
            screen.text(param_name)
            
            -- Draw parameter value
            if param_id and params:lookup_param(param_id) then
                local value = params:get(param_id)
                local formatted = params:string(param_id)
                screen.move(SCREEN_W - 30, y)
                screen.text(formatted)
                
                -- Draw value bar
                local spec = params:lookup_param(param_id)
                if spec then
                    local normalized = (value - spec.min) / (spec.max - spec.min)
                    local bar_width = normalized * 20
                    
                    screen.level(param_index == app.selected_param and 15 or 4)
                    screen.rect(SCREEN_W - 25, y - 4, bar_width, 2)
                    screen.fill()
                end
            end
        end
    end
    
    -- Draw scroll indicators
    if #level_params > visible_params then
        screen.level(4)
        if scroll_offset > 0 then
            -- Up arrow
            screen.move(SCREEN_W - 8, start_y - 2)
            screen.text("^")
        end
        if scroll_offset + visible_params < #level_params then
            -- Down arrow
            screen.move(SCREEN_W - 8, start_y + visible_params * LINE_HEIGHT - 2)
            screen.text("v")
        end
    end
end

function UI.draw_voice_activity(app)
    -- Draw 8 voice activity meters at bottom of screen
    local meter_width = 12
    local meter_height = 6
    local start_x = 8
    local y = SCREEN_H - 8
    
    for voice = 1, 8 do
        local x = start_x + (voice - 1) * (meter_width + 2)
        local activity = app.activity[voice] or 0
        
        -- Background
        screen.level(2)
        screen.rect(x, y - meter_height, meter_width, meter_height)
        screen.stroke()
        
        -- Activity level
        if activity > 0 then
            local fill_height = activity * meter_height
            screen.level(math.floor(activity * 15))
            screen.rect(x + 1, y - fill_height, meter_width - 2, fill_height)
            screen.fill()
        end
        
        -- Voice number
        screen.level(4)
        screen.move(x + 4, y + 8)
        screen.text(tostring(voice))
        
        -- Highlight current voice in voice mode
        if app.current_level == 4 and voice == app.selected_voice then
            screen.level(15)
            screen.rect(x - 1, y - meter_height - 1, meter_width + 2, meter_height + 10)
            screen.stroke()
        end
    end
end

function UI.draw_status(app)
    -- Draw key hints at bottom
    screen.level(4)
    screen.move(2, SCREEN_H - 2)
    
    if app.k1_held then
        screen.text("K1+K2:voice K1+K3:reset")
    else
        screen.text("K2:edit K3:default")
    end
end

-- Helper function to get parameter ID for display
function get_current_param_id_for_param(param_name, app)
    if app.current_level == 4 then
        -- Voice-specific parameter
        return param_name .. app.selected_voice
    elseif app.current_level == 3 then
        -- Pair-specific parameter
        local pair = math.ceil(app.selected_voice / 2)
        if param_name:match("[12]$") then
            return param_name  -- Already has voice number
        else
            return param_name .. pair
        end
    elseif app.current_level == 2 then
        -- Quad-specific parameter
        local quad = math.ceil(app.selected_voice / 4)
        return param_name .. quad
    else
        -- Global parameter
        return param_name
    end
end

-- Animation helpers
function UI.should_blink(timer, rate)
    rate = rate or BLINK_RATE
    return math.floor(timer / rate) % 2 == 0
end

-- Debug visualization
function UI.draw_debug_info(app)
    screen.level(4)
    screen.move(2, SCREEN_H - 20)
    screen.text("L:" .. app.current_level .. " V:" .. app.selected_voice .. " P:" .. app.selected_param)
    
    if app.edit_mode then
        screen.move(2, SCREEN_H - 10)
        screen.text("EDIT MODE")
    end
end

-- Emergency display (for error states)
function UI.draw_emergency(message)
    screen.clear()
    screen.level(15)
    screen.move(SCREEN_W/2, SCREEN_H/2)
    screen.text_center(message or "ERROR")
    screen.update()
end

return UI
