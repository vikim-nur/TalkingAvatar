package kg.nurtelecom.o.talkingavatar.data.api

import kg.nurtelecom.o.talkingavatar.data.models.AnswerResponse
import kg.nurtelecom.o.talkingavatar.data.models.QuestionRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/ask")
    suspend fun askQuestion(@Body request: QuestionRequest): AnswerResponse = AnswerResponse("Это семейный тариф, он рассчитан на группу до 3 основных номеров с возможностью подключить ещё до 2 дополнительных номеров\n" +
            "Абонентская плата 1490 сомов за 30 дней за базовый состав группы\n" +
            "Включает в себя следующие преимущества\n" +
            "Безлимитный интернет на каждый номер в группе\n" +
            "Бесплатная Wi-Fi раздача 10 ГБ на раздачу с каждого номера\n" +
            "100 минут на звонки к другим операторам Кыргызстана для всех номеров\n" +
            "Домашний проводной интернет и ТВ входят в пакет Скорость до 500 Мбит/с более 220 ТВ-каналов и 5 онлайн-кинотеатров\n" +
            "Роутер и ТВ-приставка предоставляются бесплатно на ответственное хранение")
}