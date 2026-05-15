package com.resumeai.notification.dto;

import lombok.*;

/** Response payload for unread count operations. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnreadCountResponse {

    private Long userId;
    private long unreadCount;
}



