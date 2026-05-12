-- 秒杀资格校验Lua脚本
-- 将库存判断、一人一单判断、扣减库存、记录用户购买集合、写入Redis Stream 合并为一次原子操作
--
-- KEYS[1] = seckill:stock:{voucherId}    -- 秒杀券Redis库存
-- KEYS[2] = seckill:order:{voucherId}    -- 已抢券用户集合
-- KEYS[3] = stream.orders                 -- 秒杀订单Redis Stream
-- ARGV[1] = orderId                       -- 预生成的订单ID
-- ARGV[2] = userId                        -- 当前用户ID
-- ARGV[3] = voucherId                     -- 秒杀券ID（用于写入Stream消息体）
--
-- 返回值:
--   0 = 成功，订单已入队
--   1 = 库存不足
--   2 = 重复下单

local stockKey = KEYS[1]
local orderSetKey = KEYS[2]
local streamKey = KEYS[3]
local orderId = ARGV[1]
local userId = ARGV[2]
local voucherId = ARGV[3]

-- 1. 判断库存是否充足
local stock = redis.call('GET', stockKey)
if not stock or tonumber(stock) <= 0 then
    return 1
end

-- 2. 判断用户是否已下单（一人一单）
local isMember = redis.call('SISMEMBER', orderSetKey, userId)
if isMember == 1 then
    return 2
end

-- 3. 扣减Redis库存
redis.call('DECR', stockKey)

-- 4. 记录用户已抢券
redis.call('SADD', orderSetKey, userId)

-- 5. 写入Redis Stream
redis.call('XADD', streamKey, '*', 'orderId', orderId, 'userId', userId, 'voucherId', voucherId)

-- 6. 返回成功
return 0
