package com.blikenoother.test;

import com.blikenoother.object.EmailCredential;
import com.blikenoother.utils.EmailListener;

public class Test {
    public static void main(String[] args) {
        EmailCredential emailCredential = new EmailCredential();
        emailCredential.setHost("imap.gmail.com");
        emailCredential.setPort("993");
        emailCredential.setUsername("username");
        emailCredential.setPassword("password");
        emailCredential.setFolder("Inbox");
        EmailListener.pullEmail(emailCredential);
    }
}
