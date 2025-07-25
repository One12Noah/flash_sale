/*
Navicat MySQL Data Transfer

Source Server         : 192.168.113.204_3306
Source Server Version : 50722
Source Host           : 192.168.113.204:3306
Source Database       : shop-intergral

Target Server Type    : MYSQL
Target Server Version : 50722
File Encoding         : 65001

Date: 2021-07-29 17:39:53
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for t_account_transaction
-- ----------------------------
DROP TABLE IF EXISTS `t_account_transaction`;
CREATE TABLE `t_account_transaction` (
  `tx_id` varchar(100) NOT NULL,
  `action_id` varchar(100) NOT NULL,
  `gmt_created` datetime DEFAULT NULL,
  `gmt_modified` datetime DEFAULT NULL,
  `amount` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `state` smallint(4) DEFAULT NULL,
  PRIMARY KEY (`tx_id`,`action_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of t_account_transaction
-- ----------------------------

-- ----------------------------
-- Table structure for t_usable_integral
-- ----------------------------
DROP TABLE IF EXISTS `t_usable_integral`;
CREATE TABLE `t_usable_integral` (
  `user_id` bigint(20) NOT NULL,
  `gmt_created` datetime DEFAULT NULL,
  `gmt_modified` datetime DEFAULT NULL,
  `amount` bigint(20) DEFAULT NULL,
  `freezed_amount` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of t_usable_integral
-- ----------------------------
INSERT INTO `t_usable_integral` VALUES ('13088889999', '2020-11-06 15:36:07', '2021-07-29 17:20:04', '500000', '0');

-- ----------------------------
-- Table structure for undo_log
-- ----------------------------
DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `branch_id` bigint(20) NOT NULL,
  `xid` varchar(100) NOT NULL,
  `context` varchar(128) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int(11) NOT NULL,
  `log_created` datetime NOT NULL,
  `log_modified` datetime NOT NULL,
  `ext` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of undo_log
-- ----------------------------
