ALTER TABLE `t_author` ADD COLUMN `movies` JSON DEFAULT NULL COMMENT '影视收藏及封面';

UPDATE `t_author`
SET `movies` = JSON_ARRAY(
    JSON_OBJECT('title', '鬼灭之刃', 'img', 'https://imgcn.bgmbk.tv/file/bk/7105/f9be5698e65a8d1da913861cdcdd38ac.webp?t=38256095707312958&k=1855601776069519'),
    JSON_OBJECT('title', '无职转生', 'img', 'https://imgcn3.bgmbk.tv/file/bk/11742/5fa16e4cac018f1a7f1158dbc02b52ff.webp?t=51647715930988718&k=1830260344101831'),
    JSON_OBJECT('title', '假面骑士zzz', 'img', 'https://static-a.xgcartoon.com/cover/jiamianqishizzzjiamianqishizeztzguoyu4k-shisenzhangtailang.jpg?w=230&h=280&q=100'),
    JSON_OBJECT('title', '蜘蛛侠：崭新之日', 'img', 'https://img.maohaha.xyz/?url=https://img3.doubanio.com/view/photo/l_ratio_poster/public/p2934276912.jpg'),
    JSON_OBJECT('title', '开学那天我被当成传说', 'img', 'https://cdn.duanjubaike.org/cover/2026/07/21e6cc4e-8640-42b5-abff-5b39e9efe8d9.jpg')
)
WHERE `deleted` = 0;
