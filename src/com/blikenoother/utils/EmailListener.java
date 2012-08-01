package com.blikenoother.utils;

import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.search.FlagTerm;

import org.apache.log4j.Logger;

import com.blikenoother.object.EmailCredential;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;

public class EmailListener {
    private static final Logger   logger            = Logger.getLogger(EmailListener.class);
    private static long           TIMEOUT           = 9 * 60 * 1000;
    private static final FlagTerm UNREAD_FLAG       = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
    private static final int      MAX_EMAIL_TO_READ = 5;

    public static void pullEmail(EmailCredential emailCredential) {
        EmailPullerThread thread = new EmailPullerThread(emailCredential);
        thread.start();
    }

    private static class EmailPullerThread extends Thread {
        private EmailCredential emailCredential;
        private Session         session       = null;
        private IMAPStore       store         = null;
        private IMAPFolder      inbox         = null;
        private boolean         closeConn     = true;
        private int             messageClount = 0;

        public EmailPullerThread(EmailCredential emailCredential) {
            this.emailCredential = emailCredential;
        }

        @Override
        public void run() {
            Properties props = System.getProperties();
            props.setProperty("mail.imap.host", emailCredential.getHost());
            props.setProperty("mail.imap.port", emailCredential.getPort());
            while (true) {
                MessageCountListener messageCountListener = new MessageCountListener() {
                    @Override
                    public void messagesRemoved(MessageCountEvent arg0) {
                    }

                    @Override
                    public void messagesAdded(MessageCountEvent arg0) {
                        readEmail(emailCredential);
                    }
                };

                try {
                    session = Session.getDefaultInstance(props);
                    store = (IMAPStore) session.getStore("imaps");
                    store.connect(emailCredential.getHost(), emailCredential.getUsername(), emailCredential.getPassword());
                    inbox = (IMAPFolder) store.getFolder(emailCredential.getFolder());
                    inbox.open(Folder.READ_ONLY);
                    inbox.addMessageCountListener(messageCountListener);
                    // check if any unread email while closing last connection
                    readEmail(emailCredential);
                    // thread for idle command on IMAPFolder
                    PushMail pm = new PushMail();
                    pm.start();
                    // kill thread after TIMEOUT minute as email client does not
                    // responds after TIMEOUT minute
                    pm.join(TIMEOUT);
                    while (!closeConn) {
                        sleep(10000);
                    }
                    // close connection as this connection is no longer gets
                    // response from email client
                    inbox.close(false);
                    store.close();
                } catch (Exception e) {
                    logger.error("Error in email pulling thread: ", e);
                }
            }
        }

        private class PushMail extends Thread {
            @Override
            public void run() {
                try {
                    inbox.idle();
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("Error in push mail: ", e);
                }
            }
        }

        private synchronized void readEmail(EmailCredential emailCredential) {
            closeConn = false;
            try {
                Message messages[] = inbox.search(UNREAD_FLAG);
                int messageReadLimit = messages.length > MAX_EMAIL_TO_READ ? MAX_EMAIL_TO_READ : messages.length;
                if (messages.length > messageClount) {
                    for (int i = messageClount; i < messageReadLimit; i++) {
                        System.out.println(emailCredential.getUsername() + ": " + messages[i].getSubject());
                    }
                    messageClount = messages.length;
                }
            } catch (Exception e) {
                logger.error("Error while reading email: ", e);
            }
            closeConn = true;
        }
    }
    
}
