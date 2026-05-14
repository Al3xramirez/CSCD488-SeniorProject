CREATE TABLE `meeting` (
  `MeetingID` bigint NOT NULL AUTO_INCREMENT,
  `ClassCode` varchar(20) DEFAULT NULL,
  `Quarter` varchar(10) DEFAULT NULL,
  `Year` int DEFAULT NULL,
  `RequesterID` varchar(255) NOT NULL,
  `RecipientID` varchar(255) NOT NULL,
  `MeetingDate` date NOT NULL,
  `StartTime` time NOT NULL,
  `EndTime` time NOT NULL,
  `Status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `Notes` text,
  `CreatedAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`MeetingID`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci