-- Xingang Community Seed Data
-- Small dataset for local manual verification

-- 商户分类
INSERT INTO `shop_type` (`id`, `name`, `icon`, `sort`) VALUES
(1, '美食', '/icons/food.png', 1),
(2, '休闲娱乐', '/icons/entertainment.png', 2);

-- 商户（位于北京海淀中关村附近）
INSERT INTO `shop` (`id`, `name`, `type_id`, `area`, `address`, `x`, `y`, `avg_price`, `score`, `open_hours`, `comments`) VALUES
(1, '老王麻辣烫', 1, '中关村', '海淀区中关村大街1号', 116.3103, 39.9836, 2500, 4.6, '10:00-22:00', 128),
(2, '蜀九香火锅', 1, '五道口', '海淀区成府路28号', 116.3264, 39.9879, 12000, 4.8, '11:00-23:00', 320),
(3, '星聚会KTV', 2, '中关村', '海淀区中关村大街5号', 116.3152, 39.9815, 8800, 4.3, '12:00-02:00', 96);

-- 用户
INSERT INTO `user` (`id`, `phone`, `nick_name`, `icon`) VALUES
(1, '13800000001', '张三', '/icons/avatar1.png'),
(2, '13800000002', '李四', '/icons/avatar2.png');

-- 优惠券（普通券）
INSERT INTO `voucher` (`id`, `shop_id`, `title`, `sub_title`, `rules`, `pay_value`, `actual_value`, `type`, `status`) VALUES
(1, 1, '满50减10', '工作日可用', '满50元可用，每单限用一张', 5000, 1000, 0, 1),
(2, 2, '满200减50', '火锅专享', '满200元可用，每桌限用一张', 20000, 5000, 0, 1),
(3, 3, '包厢8折', '周一至周四', '包厢消费满200元可用', 0, 0, 0, 1);

-- 秒杀券
INSERT INTO `voucher` (`id`, `shop_id`, `title`, `sub_title`, `rules`, `pay_value`, `actual_value`, `type`, `status`) VALUES
(4, 1, '1元抢购麻辣烫套餐', '限量100份', '仅限周一使用', 100, 3000, 1, 1);

INSERT INTO `seckill_voucher` (`voucher_id`, `stock`, `begin_time`, `end_time`) VALUES
(4, 100, '2026-05-01 00:00:00', '2026-12-31 23:59:59');
