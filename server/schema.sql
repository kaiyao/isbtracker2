CREATE TABLE IF NOT EXISTS `busstop` (
		`id` int(11) NOT NULL,
		`name` varchar(64) DEFAULT NULL,
		`latitude` double NOT NULL,
		`longitude` double NOT NULL,
		PRIMARY KEY (`id`)
		) ENGINE=InnoDB DEFAULT CHARSET=latin1;

	CREATE TABLE IF NOT EXISTS `busstopwait` (
			`busstop` int(11) NOT NULL,
			`timestart` datetime NOT NULL,
			`waittime` int(11) NOT NULL,
			KEY `busstop_idx` (`busstop`)
			) ENGINE=InnoDB DEFAULT CHARSET=latin1;

	CREATE TABLE IF NOT EXISTS `tripsegment` (
			`bsstart` int(11) NOT NULL,
			`timestart` datetime NOT NULL,
			`bsend` int(11) NOT NULL,
			`timeend` datetime NOT NULL,
			KEY `bsend_idx` (`bsend`),
			KEY `bs_idx` (`bsstart`,`bsend`)
			) ENGINE=InnoDB DEFAULT CHARSET=latin1;


	ALTER TABLE `busstopwait`
	ADD CONSTRAINT `busstop` FOREIGN KEY (`busstop`) REFERENCES `busstop` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

	ALTER TABLE `tripsegment`
	ADD CONSTRAINT `bsend` FOREIGN KEY (`bsend`) REFERENCES `busstop` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

