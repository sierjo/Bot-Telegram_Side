package com.university.bot;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
// Кнопки
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;


import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapBot extends TelegramLongPollingBot {
    private final Map<String, UserState> userStates = new HashMap<>();

    @Override
    public String getBotUsername() {
        String naimBot = null;
        try (BufferedReader br = new BufferedReader(new FileReader("D:\\! ERASMUS YCZOBA\\SOFTWARE_DELOVERY\\Bot\\txt.txt"))) {
            naimBot = br.readLine(); // Читаем первую строку файла
        } catch (IOException e) {
            e.printStackTrace(); // Логирует ошибку при чтении файла
        }
        return naimBot;
    }

    @Override
    public String getBotToken() {
        String token = null;
        try (BufferedReader br = new BufferedReader(new FileReader("D:\\! ERASMUS YCZOBA\\SOFTWARE_DELOVERY\\Bot\\txt.txt"))) {
            br.readLine();
            token = br.readLine(); // Читает вторую строку файла
        } catch (IOException e) {
            e.printStackTrace();
        }
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String text = update.getMessage().getText();


            //  Получаем или создаем состояние пользователя
            UserState userState = userStates.getOrDefault(chatId, new UserState());
            System.out.println("It's states " + userState.getState());     // Проверка
//            System.out.println("It's message " + text);                   // Проверка
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);

            // Обработка команды /restart
            if (text.equals("/restart")) {
                userStates.remove(chatId); // Сбрасываем состояние пользователя
                sendMessage.setText("Бот перезапущен! Введите первое сообщение:");
                userState.setState(1);
                userStates.put(chatId, userState); // Сохранение состояния
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return; // Завершаем обработку, чтобы не продолжать с состояниями
            }
            switch (userState.getState()) {
                case 0: // первое сообщение
                    if (text.equals("/start")) {
                        sendMessage.setText("Введите первое сообщение");
                        userState.setState(1);
                    } else {
                        sendMessage.setText("Введите команду /start");
                    }
                    break;
                case 1: // второе сообщение
                    userState.setFirstMessage(text); // Здесь храниться первое сообщение
                    sendMessage.setText("Введите второе сообщение");
                    userState.setState(2);
                    break;
                case 2: // Пересылаем объединение сообщений
                    restartMessage(chatId, userState, update);
                    userState.setState(3);
                    break;
//                case 3: // Сообщение с вариантами ответа
//                break;
                default:
                    sendMessage.setText("Введите команду: /restart");
                    userStates.remove(chatId);
                    break;
            }

//            Сохраняем состояние пользователя
            userStates.put(chatId, userState);

            try {
                this.execute(sendMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
//      public void onUpdateReceived(Update update) {
//        String chatId = update.getMessage().getChatId().toString();
//        String text = update.getMessage().getText();
//        SendMessage sendMessage = new SendMessage();
//        sendMessage.setChatId(chatId);
//        sendMessage.setText(text);

//        try {
//            this.execute(sendMessage);
//        } catch (TelegramApiException e) {
//            throw new RuntimeException(e);
//        }
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();     /* получение Id текста сообщения для
                                                                                           изменения сообщения без пересылки */
            long chatId = update.getCallbackQuery().getMessage().getChatId();           /* получение Id чата, что-бы бот знал
                                                                                           в какой чат отсылать сообщения*/
            UserState userState = userStates.getOrDefault(chatId, new UserState());
            if (callbackData.equals("Yes_Button")) {
                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(chatId));
                message.setText("Введите первое сообщение");
                userStates.remove(String.valueOf(chatId)); // Сбрасываем состояние пользователя
                userState.setState(1);
                userStates.put(String.valueOf(chatId), userState); // Сохранение состояния
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (callbackData.equals("No_Button")) {
                String text = "End";
                EditMessageText message = new EditMessageText();
                message.setChatId(String.valueOf(chatId));
                message.setText(text);
                message.setMessageId(Math.toIntExact(messageId));

                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private void restartMessage(String chatId, UserState userState, Update update) {
        SendMessage sendMessage = new SendMessage();
        String firstMessage = userState.getFirstMessage();
        String text = update.getMessage().getText();
        String combinedMessage = firstMessage + " " + text;
        sendMessage.setChatId(chatId);
        sendMessage.setText("Результат " + combinedMessage);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Do you wont to continue?");
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData("Yes_Button");       // Модификатор позволяющий боту понять какая кнопка была нажата
        var noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData("No_Button");       // Модификатор позволяющий боту понять какая кнопка была нажата

        rowInline.add(yesButton);
        rowInline.add(noButton);

        rowsInline.add(rowInline);

        markupInline.setKeyboard(rowsInline);

        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    //    Храним состояния пользователя
    private static class UserState {
        private int state;
        private String firstMessage;


        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }

        public String getFirstMessage() {
            return firstMessage;
        }

        public void setFirstMessage(String firstMessage) {
            this.firstMessage = firstMessage;
        }

    }
}

