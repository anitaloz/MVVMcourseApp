package com.example.mvvmcourseapp.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvvmcourseapp.R
import com.example.mvvmcourseapp.SessionManager
import com.example.mvvmcourseapp.UIhelper.LangButton
import com.example.mvvmcourseapp.UIhelper.ProcessButton
import com.example.mvvmcourseapp.data.DTO.UserResponse
import com.example.mvvmcourseapp.data.models.Category
import com.example.mvvmcourseapp.data.models.Lang
import com.example.mvvmcourseapp.data.models.Option
import com.example.mvvmcourseapp.data.models.QuizQuestion
import com.example.mvvmcourseapp.data.models.User
import com.example.mvvmcourseapp.data.models.UserSettings
import com.example.mvvmcourseapp.data.repositories.QuizQuestionRepo
import com.example.mvvmcourseapp.data.repositories.UserRepo
import com.example.mvvmcourseapp.viewModels.QuizViewModel.QuizEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuViewModel(
    private val userRepo: UserRepo,
    private val quizQuestionRepo: QuizQuestionRepo,
    private val sessionManager: SessionManager,
    private val sharedViewModel: SharedViewModel
) : ViewModel() {
    val user: LiveData<User?> = sharedViewModel.user

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _events= Channel<MenuEvent>()
    val events = _events.receiveAsFlow()
    init {
        viewModelScope.launch {
            fillDb()
        }
        checkSession()
        loadLanguages()
        search("")

    }

    //Проверяем авторизацию и грузим пользователя
    private fun checkSession() {
        if (!sessionManager.isLoggedIn()) {
            _uiState.value = _uiState.value.copy(loading = false)
            return
        }

        _uiState.value = _uiState.value.copy(userLoading = true)

        viewModelScope.launch {
            try {
                val user = userRepo.getCurrentUser()
                sharedViewModel.setUser(User(user.id, user.login, user.email, ""))
                _uiState.value = _uiState.value.copy(userLoading = false)

            } catch (e: Exception) {
                // Если access истёк → пробуем обновить
                val refreshed = userRepo.refreshToken()

                if (refreshed) {
                    try {
                        val user = userRepo.getCurrentUser()
                        sharedViewModel.setUser(User(user.id, user.login, user.email, ""))
                    } catch (e: Exception) {
                        sessionManager.logout()
                        sharedViewModel.setUser(null)
                    }
                } else {
                    sessionManager.logout()
                    sharedViewModel.setUser(null)
                }

                _uiState.value = _uiState.value.copy(loading = false)
            }
        }
    }



    //Загрузка кнопок языков
    fun loadLanguages() {
        viewModelScope.launch {
            quizQuestionRepo.getLangsName()
                .flowOn(Dispatchers.IO)
                .collect { list ->
                    _uiState.value=_uiState.value.copy(langButtons = list.map {
                        LangButton(name = it, image = categoryIcon(it))
                    })
                }
        }
    }

    //Поиск категорий
    fun search(query: String) {
        viewModelScope.launch {
            quizQuestionRepo.filter(query)
                .flowOn(Dispatchers.IO)
                .collect { list ->
                    _uiState.value=_uiState.value.copy(processButtons = list.map { item ->
                        ProcessButton(
                            title = "${item.langName} : ${item.category}",
                            cardsCount = "${item.cardsCount} вопросов",
                            image = categoryIcon(item.langName)
                        )
                    })
                    _uiState.value=_uiState.value.copy(loading=false)
                }
        }
    }

    //Точный поиск по языку
    fun searchAccurate(query: String) {
        viewModelScope.launch {
            quizQuestionRepo.accurateFilter(query)
                .flowOn(Dispatchers.IO)
                .collect { list ->
                    _uiState.value=_uiState.value.copy(processButtons = list.map { item ->
                        ProcessButton(
                            title = "${item.langName} : ${item.category}",
                            cardsCount = "${item.cardsCount} вопросов",
                            image = categoryIcon(item.langName)
                        )
                    })
                    _uiState.value=_uiState.value.copy(loading=false)
                }
        }
    }

    fun navigateToSettings()
    {
        sendEvent(MenuEvent.NavigateToSettings)
    }
    fun navigateToLogin()
    {
        sendEvent(MenuEvent.NavigateToLogin)
    }

    fun navigateToStatistics()
    {
        sendEvent(MenuEvent.NavigateToStatistics)
    }
    fun navigateToQuiz(){
        sendEvent(MenuEvent.NavigateToQuiz)
    }
    fun isNavigateToQuiz():Boolean
    {
        if (sharedViewModel.user.isInitialized && sessionManager.isLoggedIn())
            return true
        return false
    }

    //Пользователь выбрал категорию → сохраняем в SharedViewModel
    fun onProcessSelected(processButton: ProcessButton) {
        Log.d("USERCLICKQUIZ", sharedViewModel.user.value.toString())
        if (!sharedViewModel.user.isInitialized || sharedViewModel.user.value==null) {
            sendEvent(MenuEvent.showError("Авторизуйтесь для начала викторины"))
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val questions: MutableList<QuizQuestion> = mutableListOf()
                val langItem = quizQuestionRepo.getLangByLangName(
                    processButton.title.split(":").first().trim()
                )
                val categoryItem = quizQuestionRepo.getCategoryByNameAndLang(
                    processButton.title.split(":").last().trim(), langItem
                )
                Log.wtf("MenuViewModel", "Category set: ${categoryItem.categoryName}")
                if (categoryItem.categoryName == "Тест на определение уровня") {
                    val questionsEnter = quizQuestionRepo.getQuizQuestionByCategoryAndLang(categoryItem, langItem)
                    Log.wtf("MenuViewModel", "Category set: ${categoryItem.id}, ${langItem.id}")
                    questions.addAll(questionsEnter)
                } else {
                    val uSettings = userRepo.getUserSettingsByLang(sharedViewModel.user.value!!, categoryItem.langId)
                    Log.d("USERSETTINGS", uSettings.toString())
                    val questionsNew = quizQuestionRepo.getQuizQuestionByCategoryAndLangAndNew(
                        categoryItem,
                        langItem,
                        uSettings
                    )//достает новые вопросы
                    val questionsRepeatable =
                        quizQuestionRepo.getQuizQuestionByCategoryAndLangAndCurDate(
                            categoryItem,
                            langItem,
                            uSettings
                        )//достает вопросы для повторения
                    questions.addAll(questionsNew)
                    questions.addAll(questionsRepeatable)
                }
                withContext(Dispatchers.Main) {
                    sharedViewModel.setQuestionList(questions)
                    sharedViewModel.setCategory(categoryItem)
                    Log.wtf(
                        "MenuViewModel",
                        "Questions: ${sharedViewModel.questionList.value!!.size}"
                    )
                    if (questions.isNotEmpty()) {
                        sendEvent(MenuEvent.NavigateToQuiz)
                    } else {
                        sendEvent(MenuEvent.showError("Нет вопросов на сегодня"))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    MenuEvent.showError("Ошибка загрузки категорий: ${e.message}")
                }
            }
        }
    }


    //Иконка для категории
    private fun categoryIcon(s: String): Int {
        return when (s) {
            "Java" -> R.drawable.java
            "Python" -> R.drawable.py
            "JavaScript" -> R.drawable.js
            else -> R.drawable.common
        }
    }

//    private suspend fun fillDb() {
//        val fll = quizQuestionRepo.isNotEmptyLang()
//        val flc = quizQuestionRepo.isNotEmptyCategories()
//        val flqq = quizQuestionRepo.isNotEmptyQuestions()
//        val flo = quizQuestionRepo.isNotEmptyOptions()
//
//        if(!fll || !flc || !flqq || !flo)
//            _uiState.value = _uiState.value.copy(loading = true)
//        else {
//            _uiState.value = _uiState.value.copy(loading = false)
//            return
//        }
//
//        if (!fll) {
//            val l1 = Lang(1, "Python")
//            val l2 = Lang(2, "Java")
//            val l3 = Lang(3, "JavaScript")
//            val l4 = Lang(4, "Kotlin")
//            val listl = listOf(l1, l2, l3, l4)
//            quizQuestionRepo.insertAllLang(listl)
//        }
//
//        if(!flc) {
//            val c1 = Category(1, "Основы", 1)
//            val c2 = Category(2, "Функции", 1)
//            val c3 = Category(3, "ООП", 1)
//            val c4 = Category(4, "Синтаксис", 2)
//            val c5 = Category(5, "Типы данных", 2)
//            val c6 = Category(6, "Асинхронность", 3)
//            val c7 = Category(7, "DOM", 3)
//            val c8 = Category(8, "Input Python", 1)  // Входные квизы
//            val c9 = Category(9, "Input Java", 2)
//            val c10 = Category(10, "Input JavaScript", 3)
//            val c11 = Category(11, "Input Kotlin", 4)
//            val listc = listOf(c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11)
//            quizQuestionRepo.insertAllCategories(listc)
//        }
//
//        if(!flqq) {
//            val qql = mutableListOf<QuizQuestion>()
//
//            // Python - Основы (20 вопросов)
//            qql.addAll(listOf(
//                QuizQuestion(1, 1, 1, "Какой тип данных в Python является неизменяемым?", "В Python неизменяемыми типами являются числа, строки и кортежи."),
//                QuizQuestion(2, 1, 1, "Как создать список от 0 до 5 в Python?", "Функция range() создает последовательность чисел, а list() преобразует её в список."),
//                QuizQuestion(3, 1, 1, "Что такое PEP 8?", "PEP 8 - это руководство по стилю кода в Python."),
//                QuizQuestion(4, 1, 1, "Как проверить тип переменной в Python?", "Функция type() возвращает тип объекта."),
//                QuizQuestion(5, 1, 1, "Что такое интерпретатор Python?", "Программа, которая выполняет Python код построчно."),
//                QuizQuestion(6, 1, 1, "Как объявить переменную в Python?", "Просто присвоить значение: x = 5"),
//                QuizQuestion(7, 1, 1, "Что такое None в Python?", "Специальный объект, обозначающий отсутствие значения."),
//                QuizQuestion(8, 1, 1, "Как работает оператор is?", "Проверяет, ссылаются ли переменные на один объект."),
//                QuizQuestion(9, 1, 1, "Что такое docstring?", "Строка документации для функций, классов и модулей."),
//                QuizQuestion(10, 1, 1, "Как импортировать модуль в Python?", "С помощью ключевого слова import."),
//                QuizQuestion(11, 1, 1, "Что такое виртуальное окружение?", "Изолированная среда для установки пакетов Python."),
//                QuizQuestion(12, 1, 1, "Как работает оператор in?", "Проверяет наличие элемента в последовательности."),
//                QuizQuestion(13, 1, 1, "Что такое список (list)?", "Изменяемая последовательность элементов."),
//                QuizQuestion(14, 1, 1, "Что такое кортеж (tuple)?", "Неизменяемая последовательность элементов."),
//                QuizQuestion(15, 1, 1, "Как создать словарь?", "Через фигурные скобки: {ключ: значение}"),
//                QuizQuestion(16, 1, 1, "Что такое множество (set)?", "Неупорядоченная коллекция уникальных элементов."),
//                QuizQuestion(17, 1, 1, "Как работает срез списка?", "list[start:stop:step] - извлекает часть списка."),
//                QuizQuestion(18, 1, 1, "Что такое bool?", "Логический тип данных (True/False)."),
//                QuizQuestion(19, 1, 1, "Как преобразовать строку в число?", "С помощью int() или float()."),
//                QuizQuestion(20, 1, 1, "Что такое индексация?", "Обращение к элементам по их позиции.")
//            ))
//
//            // Python - Функции (15 вопросов)
//            qql.addAll(listOf(
//                QuizQuestion(21, 1, 2, "Как объявить функцию с аргументами по умолчанию?", "Аргументы по умолчанию указываются после имени параметра через знак равенства."),
//                QuizQuestion(22, 1, 2, "Что такое *args?", "Позволяет передавать произвольное количество позиционных аргументов."),
//                QuizQuestion(23, 1, 2, "Что такое **kwargs?", "Позволяет передавать произвольное количество именованных аргументов."),
//                QuizQuestion(24, 1, 2, "Как работает return?", "Возвращает значение из функции."),
//                QuizQuestion(25, 1, 2, "Что такое lambda-функция?", "Анонимная функция, определяемая в одной строке."),
//                QuizQuestion(26, 1, 2, "Что такое область видимости?", "Определяет, где переменная может быть использована."),
//                QuizQuestion(27, 1, 2, "Что такое global?", "Ключевое слово для изменения глобальной переменной внутри функции."),
//                QuizQuestion(28, 1, 2, "Что такое nonlocal?", "Ключевое слово для изменения переменной из внешней области видимости."),
//                QuizQuestion(29, 1, 2, "Как передаются аргументы в Python?", "По присваиванию (assignment)."),
//                QuizQuestion(30, 1, 2, "Что такое декоратор?", "Функция, которая изменяет поведение другой функции."),
//                QuizQuestion(31, 1, 2, "Что такое генератор?", "Функция, которая возвращает итератор."),
//                QuizQuestion(32, 1, 2, "Как работает yield?", "Возвращает значение и приостанавливает выполнение функции."),
//                QuizQuestion(33, 1, 2, "Что такое рекурсия?", "Когда функция вызывает саму себя."),
//                QuizQuestion(34, 1, 2, "Что такое замыкание?", "Функция, которая запоминает окружение, в котором была создана."),
//                QuizQuestion(35, 1, 2, "Что такое first-class functions?", "Функции, которые можно передавать как аргументы и возвращать.")
//            ))
//
//            // Python - ООП (15 вопросов)
//            qql.addAll(listOf(
//                QuizQuestion(36, 1, 3, "Как объявить класс в Python?", "Классы в Python объявляются с помощью ключевого слова class."),
//                QuizQuestion(37, 1, 3, "Как объявить приватный атрибут?", "Двойное подчеркивание перед именем: __attr"),
//                QuizQuestion(38, 1, 3, "Что такое self?", "Ссылка на экземпляр класса."),
//                QuizQuestion(39, 1, 3, "Что такое конструктор?", "Метод __init__, вызываемый при создании объекта."),
//                QuizQuestion(40, 1, 3, "Что такое наследование?", "Когда класс наследует атрибуты и методы другого класса."),
//                QuizQuestion(41, 1, 3, "Что такое инкапсуляция?", "Сокрытие внутренней реализации класса."),
//                QuizQuestion(42, 1, 3, "Что такое полиморфизм?", "Возможность использовать объекты разных классов одинаковым образом."),
//                QuizQuestion(43, 1, 3, "Что такое метод класса?", "Метод, который работает с классом, а не с экземпляром."),
//                QuizQuestion(44, 1, 3, "Что такое статический метод?", "Метод, который не получает неявных аргументов."),
//                QuizQuestion(45, 1, 3, "Что такое property?", "Способ контролировать доступ к атрибутам."),
//                QuizQuestion(46, 1, 3, "Что такое абстрактный класс?", "Класс, который нельзя инстанциировать напрямую."),
//                QuizQuestion(47, 1, 3, "Что такое MRO?", "Method Resolution Order - порядок разрешения методов."),
//                QuizQuestion(48, 1, 3, "Что такое миксины?", "Классы, предназначенные для добавления функциональности другим классам."),
//                QuizQuestion(49, 1, 3, "Что такое деструктор?", "Метод __del__, вызываемый при удалении объекта."),
//                QuizQuestion(50, 1, 3, "Что такое магические методы?", "Методы с двойным подчеркиванием, например __str__.")
//            ))
//
//            // Java - Синтаксис (20 вопросов)
//            qql.addAll(listOf(
//                QuizQuestion(51, 2, 4, "Как объявить переменную в Java?", "В Java сначала указывается тип, затем имя переменной."),
//                QuizQuestion(52, 2, 4, "Как создать массив в Java?", "В Java массивы создаются с указанием типа и размера."),
//                QuizQuestion(53, 2, 4, "Что такое main метод?", "Точка входа в Java приложение."),
//                QuizQuestion(54, 2, 4, "Что такое package?", "Механизм для организации классов в пространства имен."),
//                QuizQuestion(55, 2, 4, "Что такое import?", "Директива для включения классов из других пакетов."),
//                QuizQuestion(56, 2, 4, "Что такое final?", "Ключевое слово для создания констант и запрета наследования."),
//                QuizQuestion(57, 2, 4, "Что такое static?", "Ключевое слово для создания членов класса, а не экземпляра."),
//                QuizQuestion(58, 2, 4, "Как работает switch?", "Позволяет выбирать между несколькими вариантами выполнения."),
//                QuizQuestion(59, 2, 4, "Что такое цикл for?", "Цикл с известным количеством итераций."),
//                QuizQuestion(60, 2, 4, "Что такое цикл while?", "Цикл с условием продолжения."),
//                QuizQuestion(61, 2, 4, "Что такое do-while?", "Цикл, который выполняется хотя бы один раз."),
//                QuizQuestion(62, 2, 4, "Что такое break?", "Прерывает выполнение цикла или switch."),
//                QuizQuestion(63, 2, 4, "Что такое continue?", "Переходит к следующей итерации цикла."),
//                QuizQuestion(64, 2, 4, "Что такое ternary operator?", "Условный оператор: условие ? значение1 : значение2"),
//                QuizQuestion(65, 2, 4, "Что такое instanceof?", "Оператор проверки типа объекта."),
//                QuizQuestion(66, 2, 4, "Что такое enum?", "Тип данных, представляющий набор именованных констант."),
//                QuizQuestion(67, 2, 4, "Что такое annotation?", "Метаданные, добавляемые к коду."),
//                QuizQuestion(68, 2, 4, "Что такое generic?", "Механизм для создания типобезопасных коллекций и методов."),
//                QuizQuestion(69, 2, 4, "Что такое varargs?", "Аргументы переменной длины в методах."),
//                QuizQuestion(70, 2, 4, "Что такое assert?", "Ключевое слово для проверки условий во время выполнения.")
//            ))
//
//            // Добавьте остальные категории по аналогии...
//            // Для демонстрации добавим еще несколько вопросов
//
//            // JavaScript - Асинхронность (15 вопросов)
//            qql.addAll(listOf(
//                QuizQuestion(71, 3, 6, "Что вернет Promise.resolve(5)?", "Promise.resolve создает успешно выполненный промис с переданным значением."),
//                QuizQuestion(72, 3, 6, "Что делает ключевое слово await?", "await приостанавливает выполнение async функции до разрешения Promise."),
//                QuizQuestion(73, 3, 6, "Что такое callback hell?", "Ситуация, когда множество вложенных callback-функций делает код нечитаемым."),
//                // ... добавьте еще вопросы
//            ))
//
//            // Input Python (20 вопросов)
//            qql.addAll(listOf(
//                QuizQuestion(91, 1, 8, "Как установить Python на Windows?", "Скачать установщик с python.org и следовать инструкциям."),
//                QuizQuestion(92, 1, 8, "Что такое pip?", "Менеджер пакетов для установки библиотек Python."),
//                QuizQuestion(93, 1, 8, "Как создать виртуальное окружение?", "python -m venv myenv"),
//                // ... добавьте еще вопросы
//            ))
//
//            // Продолжайте добавлять вопросы для остальных категорий...
//            // Всего должно быть минимум 50 вопросов на категорию
//
//            quizQuestionRepo.insertAllQuizQuestions(qql)
//        }
//
//        if(!flo) {
//            val lo = mutableListOf<Option>()
//
//            // Добавьте варианты ответов для всех вопросов
//            // Для демонстрации добавим несколько примеров
//
//            // Вопрос 1
//            lo.addAll(listOf(
//                Option(1, 1, "Список (list)", false),
//                Option(2, 1, "Словарь (dict)", false),
//                Option(3, 1, "Кортеж (tuple)", true),
//                Option(4, 1, "Множество (set)", false)
//            ))
//
//            // Вопрос 2
//            lo.addAll(listOf(
//                Option(5, 2, "list(5)", false),
//                Option(6, 2, "list(range(6))", true),
//                Option(7, 2, "[0, 1, 2, 3, 4, 5]", true),
//                Option(8, 2, "range(5)", false)
//            ))
//
//            // Вопрос 3
//            lo.addAll(listOf(
//                Option(9, 3, "Руководство по стилю кода", true),
//                Option(10, 3, "Протокол улучшения Python", true),
//                Option(11, 3, "Название библиотеки", false),
//                Option(12, 3, "Тип данных", false)
//            ))
//
//            // Продолжайте добавлять варианты для всех вопросов...
//
//            quizQuestionRepo.insertAllOptions(lo)
//        }
//
//        _uiState.value = _uiState.value.copy(loading = false)
//    }

//    private suspend fun fillDb() {
//        val fll = quizQuestionRepo.isNotEmptyLang()
//        val flc = quizQuestionRepo.isNotEmptyCategories()
//        val flqq = quizQuestionRepo.isNotEmptyQuestions()
//        val flo = quizQuestionRepo.isNotEmptyOptions()
//
//        if(!fll || !flc || !flqq || !flo)
//            _uiState.value = _uiState.value.copy(loading = true)
//        else {
//            _uiState.value = _uiState.value.copy(loading = false)
//            return
//        }
//
//        if (!fll) {
//            val l1 = Lang(1, "Python")
//            val l2 = Lang(2, "Java")
//            val l3 = Lang(3, "JavaScript")
//            val listl = listOf(l1, l2, l3)
//            quizQuestionRepo.insertAllLang(listl)
//        }
//
//        if(!flc) {
//            // Python categories
//            val c1 = Category(1, "Тест на определение уровня", 1)
//            val c2 = Category(2, "Основы Python", 1)
//            val c3 = Category(3, "ООП в Python", 1)
//
//            // Java categories
//            val c4 = Category(4, "Тест на определение уровня", 2)
//            val c5 = Category(5, "Основы Java", 2)
//            val c6 = Category(6, "ООП в Java", 2)
//
//            // JavaScript categories
//            val c7 = Category(7, "Тест на определение уровня", 3)
//            val c8 = Category(8, "Основы JavaScript", 3)
//            val c9 = Category(9, "Асинхронность в JS", 3)
//
//            val listc = listOf(c1, c2, c3, c4, c5, c6, c7, c8, c9)
//            quizQuestionRepo.insertAllCategories(listc)
//        }
//
//        if(!flqq) {
//            val qql = mutableListOf<QuizQuestion>()
//            var questionId = 1
//
//            // Python - Входной тест (15 вопросов)
//            val pythonInputQuestions = listOf(
//                QuizQuestion(questionId++, 1, 1, "Что выведет print(2 ** 3)?", "** - оператор возведения в степень", 1),
//                QuizQuestion(questionId++, 1, 1, "Какой тип данных у значения 3.14?", "Числа с точкой имеют тип float", 1),
//                QuizQuestion(questionId++, 1, 1, "Как создать пустой список?", "Квадратные скобки создают список", 1),
//                QuizQuestion(questionId++, 1, 1, "Что делает метод append()?", "append добавляет элемент в конец списка", 1),
//                QuizQuestion(questionId++, 1, 1, "Как получить длину строки?", "len() возвращает длину последовательности", 1),
//                QuizQuestion(questionId++, 1, 1, "Что вернет 'hello'[1:4]?", "Срезы возвращают подстроку", 2),
//                QuizQuestion(questionId++, 1, 1, "Как объявить функцию?", "def используется для объявления функций", 1),
//                QuizQuestion(questionId++, 1, 1, "Что такое None?", "None представляет отсутствие значения", 2),
//                QuizQuestion(questionId++, 1, 1, "Как импортировать модуль?", "import используется для импорта", 1),
//                QuizQuestion(questionId++, 1, 1, "Что делает range(5)?", "range создает последовательность чисел", 2),
//                QuizQuestion(questionId++, 1, 1, "Как работает оператор in?", "in проверяет принадлежность элемента", 1),
//                QuizQuestion(questionId++, 1, 1, "Что такое tuple?", "Кортеж - неизменяемая последовательность", 2),
//                QuizQuestion(questionId++, 1, 1, "Как создать словарь?", "Фигурные скобки создают словарь", 1),
//                QuizQuestion(questionId++, 1, 1, "Что делает метод get() у словаря?", "get возвращает значение по ключу", 2),
//                QuizQuestion(questionId++, 1, 1, "Как обработать исключение?", "try/except обрабатывает исключения", 2)
//            )
//            qql.addAll(pythonInputQuestions)
//
//            // Python - Основы Python (30 вопросов)
//            val pythonBasicQuestions = listOf(
//                QuizQuestion(questionId++, 1, 2, "Что выведет: print([i for i in range(5) if i % 2 == 0])?", "List comprehension фильтрует четные числа", 2),
//                QuizQuestion(questionId++, 1, 2, "Какой результат: 'abc' * 3?", "Строки можно умножать на числа", 1),
//                QuizQuestion(questionId++, 1, 2, "Что вернет: bool('False')?", "Любая непустая строка True в bool", 2),
//                QuizQuestion(questionId++, 1, 2, "Как работает метод strip()?", "strip удаляет пробелы с обоих концов", 1),
//                QuizQuestion(questionId++, 1, 2, "Что делает оператор //?", "Целочисленное деление", 2),
//                QuizQuestion(questionId++, 1, 2, "Как отсортировать список по убыванию?", "sorted с reverse=True", 2),
//                QuizQuestion(questionId++, 1, 2, "Что такое lambda-функция?", "Анонимная функция", 2),
//                QuizQuestion(questionId++, 1, 2, "Как объединить два словаря?", "Метод update() или | в Python 3.9+", 3),
//                QuizQuestion(questionId++, 1, 2, "Что делает метод join()?", "Объединяет строки из списка", 2),
//                QuizQuestion(questionId++, 1, 2, "Как создать множество?", "Фигурные скобки или set()", 1),
//                QuizQuestion(questionId++, 1, 2, "Что такое frozenset?", "Неизменяемое множество", 3),
//                QuizQuestion(questionId++, 1, 2, "Как работает оператор is?", "Проверяет идентичность объектов", 2),
//                QuizQuestion(questionId++, 1, 2, "Что такое генератор?", "Функция с yield вместо return", 3),
//                QuizQuestion(questionId++, 1, 2, "Как сделать глубокую копию списка?", "Использовать copy.deepcopy()", 3),
//                QuizQuestion(questionId++, 1, 2, "Что делает функция enumerate()?", "Возвращает индекс и значение", 2),
//                QuizQuestion(questionId++, 1, 2, "Как работает оператор with?", "Контекстный менеджер для ресурсов", 2),
//                QuizQuestion(questionId++, 1, 2, "Что такое декоратор?", "Функция, изменяющая поведение другой функции", 3),
//                QuizQuestion(questionId++, 1, 2, "Как проверить наличие файла?", "os.path.exists() или pathlib", 2),
//                QuizQuestion(questionId++, 1, 2, "Что делает метод split()?", "Разбивает строку на список по разделителю", 1),
//                QuizQuestion(questionId++, 1, 2, "Как работает zip()?", "Объединяет элементы нескольких последовательностей", 2),
//                QuizQuestion(questionId++, 1, 2, "Что такое *args и **kwargs?", "Аргументы переменной длины", 3),
//                QuizQuestion(questionId++, 1, 2, "Как создать виртуальное окружение?", "python -m venv myenv", 2),
//                QuizQuestion(questionId++, 1, 2, "Что делает метод format() у строк?", "Форматирование строк", 2),
//                QuizQuestion(questionId++, 1, 2, "Как читать файл построчно?", "Использовать with open() и цикл for", 2),
//                QuizQuestion(questionId++, 1, 2, "Что такое модуль collections?", "Содержит специализированные типы данных", 3),
//                QuizQuestion(questionId++, 1, 2, "Как работает map()?", "Применяет функцию к каждому элементу", 2),
//                QuizQuestion(questionId++, 1, 2, "Что такое итератор?", "Объект с методами __iter__ и __next__", 3),
//                QuizQuestion(questionId++, 1, 2, "Как работает filter()?", "Фильтрует элементы по условию", 2),
//                QuizQuestion(questionId++, 1, 2, "Что такое list comprehension?", "Компактное создание списков", 2),
//                QuizQuestion(questionId++, 1, 2, "Как преобразовать строку в число?", "int() или float()", 1)
//            )
//            qql.addAll(pythonBasicQuestions)
//
//            // Python - ООП в Python (30 вопросов)
//            val pythonOopQuestions = listOf(
//                QuizQuestion(questionId++, 1, 3, "Как объявить класс в Python?", "class MyClass:", 1),
//                QuizQuestion(questionId++, 1, 3, "Что такое self в методах класса?", "Ссылка на экземпляр класса", 2),
//                QuizQuestion(questionId++, 1, 3, "Как создать приватный атрибут?", "__attr (двойное подчеркивание)", 2),
//                QuizQuestion(questionId++, 1, 3, "Что такое наследование?", "Создание класса на основе существующего", 2),
//                QuizQuestion(questionId++, 1, 3, "Как вызвать метод родительского класса?", "super().method()", 2),
//                QuizQuestion(questionId++, 1, 3, "Что такое полиморфизм?", "Разное поведение методов с одним именем", 3),
//                QuizQuestion(questionId++, 1, 3, "Как создать абстрактный класс?", "from abc import ABC, abstractmethod", 3),
//                QuizQuestion(questionId++, 1, 3, "Что такое метод __init__?", "Конструктор класса", 1),
//                QuizQuestion(questionId++, 1, 3, "Как работает __str__?", "Возвращает строковое представление объекта", 2),
//                QuizQuestion(questionId++, 1, 3, "Что такое property?", "Свойство класса с getter/setter", 3),
//                QuizQuestion(questionId++, 1, 3, "Как создать статический метод?", "@staticmethod декоратор", 2),
//                QuizQuestion(questionId++, 1, 3, "Что такое классовый метод?", "@classmethod с параметром cls", 3),
//                QuizQuestion(questionId++, 1, 3, "Как работает множественное наследование?", "Класс может наследовать от нескольких классов", 3),
//                QuizQuestion(questionId++, 1, 3, "Что такое MRO?", "Method Resolution Order - порядок разрешения методов", 3),
//                QuizQuestion(questionId++, 1, 3, "Как создать перечисление?", "from enum import Enum", 2),
//                QuizQuestion(questionId++, 1, 3, "Что такое dataclass?", "Автоматическая генерация методов для классов данных", 3),
//                QuizQuestion(questionId++, 1, 3, "Как работает __getattr__?", "Вызывается при обращении к несуществующему атрибуту", 3),
//                QuizQuestion(questionId++, 1, 3, "Что такое дескриптор?", "Объект с методами __get__, __set__, __delete__", 3),
//                QuizQuestion(questionId++, 1, 3, "Как создать синглтон?", "Переопределить __new__ метод", 3),
//                QuizQuestion(questionId++, 1, 3, "Что такое миксин?", "Класс для добавления функциональности другим классам", 3),
//                QuizQuestion(questionId++, 1, 3, "Как работает __call__?", "Позволяет вызывать объекты как функции", 3),
//                QuizQuestion(questionId++, 1, 3, "Что такое инкапсуляция?", "Сокрытие внутренней реализации", 2),
//                QuizQuestion(questionId++, 1, 3, "Как создать интерфейс?", "Использовать абстрактные классы", 3),
//                QuizQuestion(questionId++, 1, 3, "Что такое композиция?", "Создание объектов из других объектов", 3),
//                QuizQuestion(questionId++, 1, 3, "Как работает __slots__?", "Ограничивает атрибуты экземпляра", 3),
//                QuizQuestion(questionId++, 1, 3, "Что такое метакласс?", "Класс, создающий классы", 3),
//                QuizQuestion(questionId++, 1, 3, "Как создать иммутабельный класс?", "Использовать __slots__ и свойства", 3),
//                QuizQuestion(questionId++, 1, 3, "Что такое деструктор?", "__del__ метод", 2),
//                QuizQuestion(questionId++, 1, 3, "Как работает @property?", "Создает свойство с getter методом", 2),
//                QuizQuestion(questionId++, 1, 3, "Что такое Dunder методы?", "Методы с двойным подчеркиванием", 2)
//            )
//            qql.addAll(pythonOopQuestions)
//
//            // Java - Входной тест (15 вопросов)
//            val javaInputQuestions = listOf(
//                QuizQuestion(questionId++, 2, 4, "Как объявить переменную в Java?", "Тип имя = значение;", 1),
//                QuizQuestion(questionId++, 2, 4, "Что такое main метод?", "Точка входа в программу", 1),
//                QuizQuestion(questionId++, 2, 4, "Как создать массив?", "int[] arr = new int[5];", 1),
//                QuizQuestion(questionId++, 2, 4, "Что выведет System.out.println(5 + \"5\")?", "Конкатенация строк", 2),
//                QuizQuestion(questionId++, 2, 4, "Как объявить класс?", "public class MyClass {}", 1),
//                QuizQuestion(questionId++, 2, 4, "Что такое package?", "Пространство имен для классов", 2),
//                QuizQuestion(questionId++, 2, 4, "Как работает цикл for?", "for(инициализация; условие; инкремент)", 1),
//                QuizQuestion(questionId++, 2, 4, "Что такое ArrayList?", "Динамический массив", 2),
//                QuizQuestion(questionId++, 2, 4, "Как сравнить строки?", "equals() вместо ==", 2),
//                QuizQuestion(questionId++, 2, 4, "Что такое final?", "Константа или запрет наследования", 2),
//                QuizQuestion(questionId++, 2, 4, "Как обработать исключение?", "try-catch блок", 2),
//                QuizQuestion(questionId++, 2, 4, "Что такое interface?", "Контракт без реализации", 2),
//                QuizQuestion(questionId++, 2, 4, "Как создать объект?", "new MyClass()", 1),
//                QuizQuestion(questionId++, 2, 4, "Что такое static?", "Принадлежит классу, а не экземпляру", 2),
//                QuizQuestion(questionId++, 2, 4, "Как наследовать класс?", "extends keyword", 2)
//            )
//            qql.addAll(javaInputQuestions)
//
//            // Java - Основы Java (30 вопросов)
//            val javaBasicQuestions = listOf(
//                QuizQuestion(questionId++, 2, 5, "Что такое JVM?", "Виртуальная машина Java", 1),
//                QuizQuestion(questionId++, 2, 5, "Как работает garbage collection?", "Автоматическое освобождение памяти", 2),
//                QuizQuestion(questionId++, 2, 5, "Что такое bytecode?", "Промежуточный код для JVM", 2),
//                QuizQuestion(questionId++, 2, 5, "Как объявить константу?", "static final", 2),
//                QuizQuestion(questionId++, 2, 5, "Что такое autoboxing?", "Автоматическая упаковка примитивов", 3),
//                QuizQuestion(questionId++, 2, 5, "Как работает switch с строками?", "С Java 7 можно использовать String", 2),
//                QuizQuestion(questionId++, 2, 5, "Что такое varargs?", "Аргументы переменной длины", 3),
//                QuizQuestion(questionId++, 2, 5, "Как создать immutable класс?", "final класс, final поля, нет сеттеров", 3),
//                QuizQuestion(questionId++, 2, 5, "Что такое enum?", "Перечисление констант", 2),
//                QuizQuestion(questionId++, 2, 5, "Как работает try-with-resources?", "Автоматическое закрытие ресурсов", 2),
//                QuizQuestion(questionId++, 2, 5, "Что такое аннотации?", "Метаданные для кода", 2),
//                QuizQuestion(questionId++, 2, 5, "Как создать поток?", "Thread класс или Runnable", 2),
//                QuizQuestion(questionId++, 2, 5, "Что такое synchronized?", "Синхронизация потоков", 3),
//                QuizQuestion(questionId++, 2, 5, "Как работает HashMap?", "Хеш-таблица для ключ-значение", 2),
//                QuizQuestion(questionId++, 2, 5, "Что такое generics?", "Обобщенные типы", 3),
//                QuizQuestion(questionId++, 2, 5, "Как создать список?", "List<String> list = new ArrayList<>()", 2),
//                QuizQuestion(questionId++, 2, 5, "Что такое Optional?", "Контейнер для nullable значений", 3),
//                QuizQuestion(questionId++, 2, 5, "Как работает Stream API?", "Функциональные операции над коллекциями", 3),
//                QuizQuestion(questionId++, 2, 5, "Что такое lambda выражения?", "Анонимные функции", 2),
//                QuizQuestion(questionId++, 2, 5, "Как сравнивать объекты?", "Comparable или Comparator", 3),
//                QuizQuestion(questionId++, 2, 5, "Что такое reflection?", "Анализ классов во время выполнения", 3),
//                QuizQuestion(questionId++, 2, 5, "Как создать файл?", "File класс или NIO", 2),
//                QuizQuestion(questionId++, 2, 5, "Что такое serialization?", "Преобразование объекта в байты", 3),
//                QuizQuestion(questionId++, 2, 5, "Как работает hashCode()?", "Возвращает хеш-код объекта", 2),
//                QuizQuestion(questionId++, 2, 5, "Что такое finalize()?", "Метод вызываемый перед удалением объекта", 3),
//                QuizQuestion(questionId++, 2, 5, "Как создать дату?", "LocalDate.now()", 2),
//                QuizQuestion(questionId++, 2, 5, "Что такое JDK?", "Java Development Kit", 1),
//                QuizQuestion(questionId++, 2, 5, "Как работает instanceof?", "Проверяет тип объекта", 2),
//                QuizQuestion(questionId++, 2, 5, "Что такое package-private?", "Доступ в пределах пакета", 2),
//                QuizQuestion(questionId++, 2, 5, "Как объявить массив?", "int[] arr или int arr[]", 1)
//            )
//            qql.addAll(javaBasicQuestions)
//
//            // Java - ООП в Java (30 вопросов)
//            val javaOopQuestions = listOf(
//                QuizQuestion(questionId++, 2, 6, "Что такое инкапсуляция?", "Сокрытие реализации", 2),
//                QuizQuestion(questionId++, 2, 6, "Как работает наследование?", "extends keyword", 2),
//                QuizQuestion(questionId++, 2, 6, "Что такое полиморфизм?", "Разные реализации методов", 3),
//                QuizQuestion(questionId++, 2, 6, "Как создать абстрактный класс?", "abstract class", 2),
//                QuizQuestion(questionId++, 2, 6, "Что такое интерфейс?", "Контракт без реализации", 2),
//                QuizQuestion(questionId++, 2, 6, "Как работает super()?", "Вызов конструктора родителя", 2),
//                QuizQuestion(questionId++, 2, 6, "Что такое конструктор?", "Метод инициализации объекта", 1),
//                QuizQuestion(questionId++, 2, 6, "Как перегрузить метод?", "Метод с тем же именем но разными параметрами", 2),
//                QuizQuestion(questionId++, 2, 6, "Что такое переопределение?", "Изменение метода родителя в потомке", 2),
//                QuizQuestion(questionId++, 2, 6, "Как создать внутренний класс?", "Class внутри класса", 3),
//                QuizQuestion(questionId++, 2, 6, "Что такое статический класс?", "Вложенный static класс", 3),
//                QuizQuestion(questionId++, 2, 6, "Как работает final с классами?", "Запрещает наследование", 2),
//                QuizQuestion(questionId++, 2, 6, "Что такое абстрактный метод?", "Метод без реализации", 2),
//                QuizQuestion(questionId++, 2, 6, "Как создать singleton?", "private конструктор, static instance", 3),
//                QuizQuestion(questionId++, 2, 6, "Что такое factory pattern?", "Паттерн создания объектов", 3),
//                QuizQuestion(questionId++, 2, 6, "Как работает builder pattern?", "Постепенное создание сложных объектов", 3),
//                QuizQuestion(questionId++, 2, 6, "Что такое dependency injection?", "Внедрение зависимостей", 3),
//                QuizQuestion(questionId++, 2, 6, "Как создать immutable объект?", "final поля, нет сеттеров", 3),
//                QuizQuestion(questionId++, 2, 6, "Что такое композиция?", "Создание объектов из других объектов", 3),
//                QuizQuestion(questionId++, 2, 6, "Как работает instanceof?", "Проверка типа объекта", 2),
//                QuizQuestion(questionId++, 2, 6, "Что такое covariant return type?", "Возврат подтипа при переопределении", 3),
//                QuizQuestion(questionId++, 2, 6, "Как создать анонимный класс?", "new Interface() { методы }", 3),
//                QuizQuestion(questionId++, 2, 6, "Что такое marker interface?", "Интерфейс без методов", 3),
//                QuizQuestion(questionId++, 2, 6, "Как работает clone()?", "Создание копии объекта", 3),
//                QuizQuestion(questionId++, 2, 6, "Что такое sealed classes?", "Классы с ограниченным наследованием", 3),
//                QuizQuestion(questionId++, 2, 6, "Как создать record?", "record Point(int x, int y)", 3),
//                QuizQuestion(questionId++, 2, 6, "Что такое static factory method?", "Статический метод создания объекта", 3),
//                QuizQuestion(questionId++, 2, 6, "Как работает this?", "Ссылка на текущий объект", 2),
//                QuizQuestion(questionId++, 2, 6, "Что такое method overloading?", "Методы с одним именем но разными параметрами", 2),
//                QuizQuestion(questionId++, 2, 6, "Как создать enum с методами?", "enum с телом и методами", 3)
//            )
//            qql.addAll(javaOopQuestions)
//
//            // JavaScript - Входной тест (15 вопросов)
//            val jsInputQuestions = listOf(
//                QuizQuestion(questionId++, 3, 7, "Как объявить переменную?", "let, const, var", 1),
//                QuizQuestion(questionId++, 3, 7, "Что выведет console.log(2 + '2')?", "Конкатенация строк", 2),
//                QuizQuestion(questionId++, 3, 7, "Как создать массив?", "[] или new Array()", 1),
//                QuizQuestion(questionId++, 3, 7, "Что такое typeof?", "Оператор проверки типа", 1),
//                QuizQuestion(questionId++, 3, 7, "Как объявить функцию?", "function name() или const name = () =>", 1),
//                QuizQuestion(questionId++, 3, 7, "Что такое DOM?", "Document Object Model", 1),
//                QuizQuestion(questionId++, 3, 7, "Как выбрать элемент по id?", "document.getElementById()", 1),
//                QuizQuestion(questionId++, 3, 7, "Что такое event listener?", "Обработчик событий", 2),
//                QuizQuestion(questionId++, 3, 7, "Как работает ===?", "Строгое сравнение без приведения типов", 2),
//                QuizQuestion(questionId++, 3, 7, "Что такое JSON?", "JavaScript Object Notation", 1),
//                QuizQuestion(questionId++, 3, 7, "Как преобразовать в строку?", "JSON.stringify() или toString()", 2),
//                QuizQuestion(questionId++, 3, 7, "Что такое closure?", "Замыкание - функция с запомненным окружением", 3),
//                QuizQuestion(questionId++, 3, 7, "Как работает setTimeout?", "Выполняет функцию после задержки", 2),
//                QuizQuestion(questionId++, 3, 7, "Что такое this?", "Контекст выполнения", 2),
//                QuizQuestion(questionId++, 3, 7, "Как создать объект?", "{} или new Object()", 1)
//            )
//            qql.addAll(jsInputQuestions)
//
//            // JavaScript - Основы JavaScript (30 вопросов)
//            val jsBasicQuestions = listOf(
//                QuizQuestion(questionId++, 3, 8, "Что такое hoisting?", "Поднятие переменных и функций", 2),
//                QuizQuestion(questionId++, 3, 8, "Как работает let vs var?", "Блочная vs функциональная область видимости", 2),
//                QuizQuestion(questionId++, 3, 8, "Что такое arrow function?", "Стрелочные функции", 2),
//                QuizQuestion(questionId++, 3, 8, "Как работает destructuring?", "Разбор объектов и массивов", 2),
//                QuizQuestion(questionId++, 3, 8, "Что такое template literals?", "Строки с для переменных", 1),
//                QuizQuestion(questionId++, 3, 8, "Как работает spread operator?", "... для копирования и объединения", 2),
//                QuizQuestion(questionId++, 3, 8, "Что такое rest parameters?", "... для аргументов функции", 2),
//                QuizQuestion(questionId++, 3, 8, "Как создать класс?", "class MyClass {}", 2),
//                QuizQuestion(questionId++, 3, 8, "Что такое prototype?", "Механизм наследования в JS", 3),
//                QuizQuestion(questionId++, 3, 8, "Как работает inheritance?", "extends keyword", 2),
//                QuizQuestion(questionId++, 3, 8, "Что такое callback?", "Функция передаваемая как аргумент", 2),
//                QuizQuestion(questionId++, 3, 8, "Как работает map()?", "Преобразование элементов массива", 2),
//                QuizQuestion(questionId++, 3, 8, "Что такое filter()?", "Фильтрация элементов массива", 2),
//                QuizQuestion(questionId++, 3, 8, "Как работает reduce()?", "Агрегация элементов массива", 3),
//                QuizQuestion(questionId++, 3, 8, "Что такое Promise?", "Обещание будущего результата", 2),
//                QuizQuestion(questionId++, 3, 8, "Как работает async/await?", "Синтаксис для работы с асинхронностью", 2),
//                QuizQuestion(questionId++, 3, 8, "Что такое event loop?", "Механизм обработки событий", 3),
//                QuizQuestion(questionId++, 3, 8, "Как работает call stack?", "Стек вызовов функций", 3),
//                QuizQuestion(questionId++, 3, 8, "Что такое callback hell?", "Вложенные колбэки", 2),
//                QuizQuestion(questionId++, 3, 8, "Как создать модуль?", "export/import", 2),
//                QuizQuestion(questionId++, 3, 8, "Что такое IIFE?", "Немедленно вызываемая функция", 2),
//                QuizQuestion(questionId++, 3, 8, "Как работает bind()?", "Привязка контекста", 2),
//                QuizQuestion(questionId++, 3, 8, "Что такое apply() и call()?", "Вызов функции с указанным контекстом", 2),
//                QuizQuestion(questionId++, 3, 8, "Как работает Object.create()?", "Создание объекта с указанным прототипом", 3),
//                QuizQuestion(questionId++, 3, 8, "Что такое Symbol?", "Уникальный и неизменяемый идентификатор", 3),
//                QuizQuestion(questionId++, 3, 8, "Как работает Proxy?", "Перехват операций над объектом", 3),
//                QuizQuestion(questionId++, 3, 8, "Что такое Reflect?", "Методы для метапрограммирования", 3),
//                QuizQuestion(questionId++, 3, 8, "Как работает generator?", "Функция с yield", 3),
//                QuizQuestion(questionId++, 3, 8, "Что такое WeakMap?", "Map со слабыми ссылками", 3),
//                QuizQuestion(questionId++, 3, 8, "Как работает optional chaining?", "?. для безопасного доступа", 2)
//            )
//            qql.addAll(jsBasicQuestions)
//
//            // JavaScript - Асинхронность в JS (30 вопросов)
//            val jsAsyncQuestions = listOf(
//                QuizQuestion(questionId++, 3, 9, "Что такое Promise?", "Обещание будущего значения", 2),
//                QuizQuestion(questionId++, 3, 9, "Как создать Promise?", "new Promise((resolve, reject) => {})", 2),
//                QuizQuestion(questionId++, 3, 9, "Что такое then/catch?", "Обработка успеха и ошибок Promise", 2),
//                QuizQuestion(questionId++, 3, 9, "Как работает async/await?", "Синтаксический сахар для Promise", 2),
//                QuizQuestion(questionId++, 3, 9, "Что такое Promise.all?", "Ожидание всех Promise", 2),
//                QuizQuestion(questionId++, 3, 9, "Как работает Promise.race?", "Ожидание первого завершенного Promise", 2),
//                QuizQuestion(questionId++, 3, 9, "Что такое Promise.allSettled?", "Ожидание всех Promise с результатами", 3),
//                QuizQuestion(questionId++, 3, 9, "Как работает Promise.any?", "Ожидание первого успешного Promise", 3),
//                QuizQuestion(questionId++, 3, 9, "Что такое microtask queue?", "Очередь микрозадач", 3),
//                QuizQuestion(questionId++, 3, 9, "Как работает event loop?", "Цикл событий", 3),
//                QuizQuestion(questionId++, 3, 9, "Что такое callback queue?", "Очередь колбэков", 3),
//                QuizQuestion(questionId++, 3, 9, "Как работает setTimeout?", "Макрозадача с задержкой", 2),
//                QuizQuestion(questionId++, 3, 9, "Что такое setImmediate?", "Макрозадача без задержки (Node.js)", 3),
//                QuizQuestion(questionId++, 3, 9, "Как работает process.nextTick?", "Микрозадача в Node.js", 3),
//                QuizQuestion(questionId++, 3, 9, "Что такое async iterator?", "Итератор для асинхронных данных", 3),
//                QuizQuestion(questionId++, 3, 9, "Как работает for await...of?", "Цикл для асинхронных итераторов", 3),
//                QuizQuestion(questionId++, 3, 9, "Что такое AbortController?", "Отмена асинхронных операций", 3),
//                QuizQuestion(questionId++, 3, 9, "Как работает fetch?", "API для HTTP запросов", 2),
//                QuizQuestion(questionId++, 3, 9, "Что такое XMLHttpRequest?", "Старый API для HTTP запросов", 2),
//                QuizQuestion(questionId++, 3, 9, "Как работает Web Workers?", "Многопоточность в браузере", 3),
//                QuizQuestion(questionId++, 3, 9, "Что такое Service Worker?", "Прокси для сетевых запросов", 3),
//                QuizQuestion(questionId++, 3, 9, "Как работает async generator?", "Генератор с асинхронными yield", 3),
//                QuizQuestion(questionId++, 3, 9, "Что такое Observable?", "Поток значений (RxJS)", 3),
//                QuizQuestion(questionId++, 3, 9, "Как работает debounce?", "Отложенное выполнение функции", 2),
//                QuizQuestion(questionId++, 3, 9, "Что такое throttle?", "Ограничение частоты выполнения", 2),
//                QuizQuestion(questionId++, 3, 9, "Как создать кастомный EventEmitter?", "Класс с методами on/emit", 3),
//                QuizQuestion(questionId++, 3, 9, "Что такое Promise chaining?", "Цепочка then методов", 2),
//                QuizQuestion(questionId++, 3, 9, "Как работает async error handling?", "try/catch с await", 2),
//                QuizQuestion(questionId++, 3, 9, "Что такое unhandled promise rejection?", "Необработанная ошибка Promise", 2),
//                QuizQuestion(questionId++, 3, 9, "Как отменить fetch запрос?", "AbortController + signal", 3)
//            )
//            qql.addAll(jsAsyncQuestions)
//
//            quizQuestionRepo.insertAllQuizQuestions(qql)
//        }
//
//        if(!flo) {
//            val lo = mutableListOf<Option>()
//            var optionId = 1
//
//            // Опции для Python вопросов (1-25)
//            for (questionId in 1..225) {
//                when (questionId) {
//                    1 -> { // Что выведет print(2 ** 3)?
//                        lo.add(Option(optionId++, questionId, "8", true))
//                        lo.add(Option(optionId++, questionId, "6", false))
//                        lo.add(Option(optionId++, questionId, "9", false))
//                        lo.add(Option(optionId++, questionId, "Ошибку", false))
//                    }
//                    2 -> { // Какой тип данных у значения 3.14?
//                        lo.add(Option(optionId++, questionId, "float", true))
//                        lo.add(Option(optionId++, questionId, "int", false))
//                        lo.add(Option(optionId++, questionId, "double", false))
//                        lo.add(Option(optionId++, questionId, "decimal", false))
//                    }
//                    3 -> { // Как создать пустой список?
//                        lo.add(Option(optionId++, questionId, "[]", true))
//                        lo.add(Option(optionId++, questionId, "list()", true))
//                        lo.add(Option(optionId++, questionId, "{}", false))
//                        lo.add(Option(optionId++, questionId, "()", false))
//                    }
//                    4 -> { // Что делает метод append()?
//                        lo.add(Option(optionId++, questionId, "Добавляет элемент в конец списка", true))
//                        lo.add(Option(optionId++, questionId, "Удаляет элемент из списка", false))
//                        lo.add(Option(optionId++, questionId, "Сортирует список", false))
//                        lo.add(Option(optionId++, questionId, "Объединяет два списка", false))
//                    }
//                    5 -> { // Как получить длину строки?
//                        lo.add(Option(optionId++, questionId, "len()", true))
//                        lo.add(Option(optionId++, questionId, "length()", false))
//                        lo.add(Option(optionId++, questionId, "size()", false))
//                        lo.add(Option(optionId++, questionId, "count()", false))
//                    }
//                    // Java вопросы (16-20)
//                    16 -> { // Как объявить переменную в Java?
//                        lo.add(Option(optionId++, questionId, "int x = 5;", true))
//                        lo.add(Option(optionId++, questionId, "var x = 5;", false))
//                        lo.add(Option(optionId++, questionId, "x := 5;", false))
//                        lo.add(Option(optionId++, questionId, "let x = 5;", false))
//                    }
//                    17 -> { // Что такое main метод?
//                        lo.add(Option(optionId++, questionId, "Точка входа в программу", true))
//                        lo.add(Option(optionId++, questionId, "Метод для вывода текста", false))
//                        lo.add(Option(optionId++, questionId, "Конструктор класса", false))
//                        lo.add(Option(optionId++, questionId, "Статический блок инициализации", false))
//                    }
//                    // JavaScript вопросы (21-25)
//                    21 -> { // Как объявить переменную?
//                        lo.add(Option(optionId++, questionId, "let x = 5;", true))
//                        lo.add(Option(optionId++, questionId, "const x = 5;", true))
//                        lo.add(Option(optionId++, questionId, "var x = 5;", true))
//                        lo.add(Option(optionId++, questionId, "int x = 5;", false))
//                    }
//                    22 -> { // Что выведет console.log(2 + '2')?
//                        lo.add(Option(optionId++, questionId, "'22'", true))
//                        lo.add(Option(optionId++, questionId, "4", false))
//                        lo.add(Option(optionId++, questionId, "NaN", false))
//                        lo.add(Option(optionId++, questionId, "Ошибку", false))
//                    }
//                    else -> {
//                        // Общие опции для остальных вопросов
//                        lo.add(Option(optionId++, questionId, "Правильный ответ", true))
//                        lo.add(Option(optionId++, questionId, "Неправильный вариант 1", false))
//                        lo.add(Option(optionId++, questionId, "Неправильный вариант 2", false))
//                        lo.add(Option(optionId++, questionId, "Неправильный вариант 3", false))
//                    }
//                }
//            }
//
//            quizQuestionRepo.insertAllOptions(lo)
//        }
//
//        _uiState.value = _uiState.value.copy(loading = false)
//    }

    private suspend fun fillDb() {
        val fll = quizQuestionRepo.isNotEmptyLang()
        val flc = quizQuestionRepo.isNotEmptyCategories()
        val flqq = quizQuestionRepo.isNotEmptyQuestions()
        val flo = quizQuestionRepo.isNotEmptyOptions()

        if(!fll || !flc || !flqq || !flo)
            _uiState.value = _uiState.value.copy(loading = true)
        else {
            _uiState.value = _uiState.value.copy(loading = false)
            return
        }

        if (!fll) {
            val l1 = Lang(1, "Python")
            val l2 = Lang(2, "Java")
            val l3 = Lang(3, "JavaScript")
            val listl = listOf(l1, l2, l3)
            quizQuestionRepo.insertAllLang(listl)
        }

        if(!flc) {
            // Python categories
            val c1 = Category(1, "Тест на определение уровня", 1)
            val c2 = Category(2, "Основы Python", 1)
            val c3 = Category(3, "ООП в Python", 1)

            // Java categories
            val c4 = Category(4, "Тест на определение уровня", 2)
            val c5 = Category(5, "Основы Java", 2)
            val c6 = Category(6, "ООП в Java", 2)

            // JavaScript categories
            val c7 = Category(7, "Тест на определение уровня", 3)
            val c8 = Category(8, "Основы JavaScript", 3)
            val c9 = Category(9, "Асинхронность в JS", 3)

            val listc = listOf(c1, c2, c3, c4, c5, c6, c7, c8, c9)
            quizQuestionRepo.insertAllCategories(listc)
        }

        if(!flqq) {
            val qql = mutableListOf<QuizQuestion>()
            var questionId = 1

            // Python - Входной тест (15 вопросов)
            val pythonInputQuestions = listOf(
                QuizQuestion(questionId++, 1, 1, "Что выведет print(2 ** 3)?", "** - оператор возведения в степень", 1),
                QuizQuestion(questionId++, 1, 1, "Какой тип данных у значения 3.14?", "Числа с точкой имеют тип float", 1),
                QuizQuestion(questionId++, 1, 1, "Как создать пустой список?", "Квадратные скобки создают список", 1),
                QuizQuestion(questionId++, 1, 1, "Что делает метод append()?", "append добавляет элемент в конец списка", 1),
                QuizQuestion(questionId++, 1, 1, "Как получить длину строки?", "len() возвращает длину последовательности", 1),
                QuizQuestion(questionId++, 1, 1, "Что вернет 'hello'[1:4]?", "Срезы возвращают подстроку", 2),
                QuizQuestion(questionId++, 1, 1, "Как объявить функцию?", "def используется для объявления функций", 1),
                QuizQuestion(questionId++, 1, 1, "Что такое None?", "None представляет отсутствие значения", 2),
                QuizQuestion(questionId++, 1, 1, "Как импортировать модуль?", "import используется для импорта", 1),
                QuizQuestion(questionId++, 1, 1, "Что делает range(5)?", "range создает последовательность чисел", 2),
                QuizQuestion(questionId++, 1, 1, "Как работает оператор in?", "in проверяет принадлежность элемента", 1),
                QuizQuestion(questionId++, 1, 1, "Что такое tuple?", "Кортеж - неизменяемая последовательность", 2),
                QuizQuestion(questionId++, 1, 1, "Как создать словарь?", "Фигурные скобки создают словарь", 1),
                QuizQuestion(questionId++, 1, 1, "Что делает метод get() у словаря?", "get возвращает значение по ключу", 2),
                QuizQuestion(questionId++, 1, 1, "Как обработать исключение?", "try/except обрабатывает исключения", 2)
            )
            qql.addAll(pythonInputQuestions)

            // Python - Основы Python (15 вопросов)
            val pythonBasicQuestions = listOf(
                QuizQuestion(questionId++, 1, 2, "Что выведет: print([i for i in range(5) if i % 2 == 0])?", "List comprehension фильтрует четные числа", 2),
                QuizQuestion(questionId++, 1, 2, "Какой результат: 'abc' * 3?", "Строки можно умножать на числа", 1),
                QuizQuestion(questionId++, 1, 2, "Что вернет: bool('False')?", "Любая непустая строка True в bool", 2),
                QuizQuestion(questionId++, 1, 2, "Как работает метод strip()?", "strip удаляет пробелы с обоих концов", 1),
                QuizQuestion(questionId++, 1, 2, "Что делает оператор //?", "Целочисленное деление", 2),
                QuizQuestion(questionId++, 1, 2, "Как отсортировать список по убыванию?", "sorted с reverse=True", 2),
                QuizQuestion(questionId++, 1, 2, "Что такое lambda-функция?", "Анонимная функция", 2),
                QuizQuestion(questionId++, 1, 2, "Как объединить два словаря?", "Метод update() или | в Python 3.9+", 3),
                QuizQuestion(questionId++, 1, 2, "Что делает метод join()?", "Объединяет строки из списка", 2),
                QuizQuestion(questionId++, 1, 2, "Как создать множество?", "Фигурные скобки или set()", 1),
                QuizQuestion(questionId++, 1, 2, "Что такое frozenset?", "Неизменяемое множество", 3),
                QuizQuestion(questionId++, 1, 2, "Как работает оператор is?", "Проверяет идентичность объектов", 2),
                QuizQuestion(questionId++, 1, 2, "Что такое генератор?", "Функция с yield вместо return", 3),
                QuizQuestion(questionId++, 1, 2, "Как сделать глубокую копию списка?", "Использовать copy.deepcopy()", 3),
                QuizQuestion(questionId++, 1, 2, "Что делает функция enumerate()?", "Возвращает индекс и значение", 2)
            )
            qql.addAll(pythonBasicQuestions)

            // Python - ООП в Python (15 вопросов)
            val pythonOopQuestions = listOf(
                QuizQuestion(questionId++, 1, 3, "Как объявить класс в Python?", "class MyClass:", 1),
                QuizQuestion(questionId++, 1, 3, "Что такое self в методах класса?", "Ссылка на экземпляр класса", 2),
                QuizQuestion(questionId++, 1, 3, "Как создать приватный атрибут?", "__attr (двойное подчеркивание)", 2),
                QuizQuestion(questionId++, 1, 3, "Что такое наследование?", "Создание класса на основе существующего", 2),
                QuizQuestion(questionId++, 1, 3, "Как вызвать метод родительского класса?", "super().method()", 2),
                QuizQuestion(questionId++, 1, 3, "Что такое полиморфизм?", "Разное поведение методов с одним именем", 3),
                QuizQuestion(questionId++, 1, 3, "Как создать абстрактный класс?", "from abc import ABC, abstractmethod", 3),
                QuizQuestion(questionId++, 1, 3, "Что такое метод __init__?", "Конструктор класса", 1),
                QuizQuestion(questionId++, 1, 3, "Как работает __str__?", "Возвращает строковое представление объекта", 2),
                QuizQuestion(questionId++, 1, 3, "Что такое property?", "Свойство класса с getter/setter", 3),
                QuizQuestion(questionId++, 1, 3, "Как создать статический метод?", "@staticmethod декоратор", 2),
                QuizQuestion(questionId++, 1, 3, "Что такое классовый метод?", "@classmethod с параметром cls", 3),
                QuizQuestion(questionId++, 1, 3, "Как работает множественное наследование?", "Класс может наследовать от нескольких классов", 3),
                QuizQuestion(questionId++, 1, 3, "Что такое MRO?", "Method Resolution Order - порядок разрешения методов", 3),
                QuizQuestion(questionId++, 1, 3, "Как создать перечисление?", "from enum import Enum", 2)
            )
            qql.addAll(pythonOopQuestions)

            // Java - Входной тест (15 вопросов)
            val javaInputQuestions = listOf(
                QuizQuestion(questionId++, 2, 4, "Как объявить переменную в Java?", "Тип имя = значение;", 1),
                QuizQuestion(questionId++, 2, 4, "Что такое main метод?", "Точка входа в программу", 1),
                QuizQuestion(questionId++, 2, 4, "Как создать массив?", "int[] arr = new int[5];", 1),
                QuizQuestion(questionId++, 2, 4, "Что выведет System.out.println(5 + \"5\")?", "Конкатенация строк", 2),
                QuizQuestion(questionId++, 2, 4, "Как объявить класс?", "public class MyClass {}", 1),
                QuizQuestion(questionId++, 2, 4, "Что такое package?", "Пространство имен для классов", 2),
                QuizQuestion(questionId++, 2, 4, "Как работает цикл for?", "for(инициализация; условие; инкремент)", 1),
                QuizQuestion(questionId++, 2, 4, "Что такое ArrayList?", "Динамический массив", 2),
                QuizQuestion(questionId++, 2, 4, "Как сравнить строки?", "equals() вместо ==", 2),
                QuizQuestion(questionId++, 2, 4, "Что такое final?", "Константа или запрет наследования", 2),
                QuizQuestion(questionId++, 2, 4, "Как обработать исключение?", "try-catch блок", 2),
                QuizQuestion(questionId++, 2, 4, "Что такое interface?", "Контракт без реализации", 2),
                QuizQuestion(questionId++, 2, 4, "Как создать объект?", "new MyClass()", 1),
                QuizQuestion(questionId++, 2, 4, "Что такое static?", "Принадлежит классу, а не экземпляру", 2),
                QuizQuestion(questionId++, 2, 4, "Как наследовать класс?", "extends keyword", 2)
            )
            qql.addAll(javaInputQuestions)

            // Java - Основы Java (15 вопросов)
            val javaBasicQuestions = listOf(
                QuizQuestion(questionId++, 2, 5, "Что такое JVM?", "Виртуальная машина Java", 1),
                QuizQuestion(questionId++, 2, 5, "Как работает garbage collection?", "Автоматическое освобождение памяти", 2),
                QuizQuestion(questionId++, 2, 5, "Что такое bytecode?", "Промежуточный код для JVM", 2),
                QuizQuestion(questionId++, 2, 5, "Как объявить константу?", "static final", 2),
                QuizQuestion(questionId++, 2, 5, "Что такое autoboxing?", "Автоматическая упаковка примитивов", 3),
                QuizQuestion(questionId++, 2, 5, "Как работает switch с строками?", "С Java 7 можно использовать String", 2),
                QuizQuestion(questionId++, 2, 5, "Что такое varargs?", "Аргументы переменной длины", 3),
                QuizQuestion(questionId++, 2, 5, "Как создать immutable класс?", "final класс, final поля, нет сеттеров", 3),
                QuizQuestion(questionId++, 2, 5, "Что такое enum?", "Перечисление констант", 2),
                QuizQuestion(questionId++, 2, 5, "Как работает try-with-resources?", "Автоматическое закрытие ресурсов", 2),
                QuizQuestion(questionId++, 2, 5, "Что такое аннотации?", "Метаданные для кода", 2),
                QuizQuestion(questionId++, 2, 5, "Как создать поток?", "Thread класс или Runnable", 2),
                QuizQuestion(questionId++, 2, 5, "Что такое synchronized?", "Синхронизация потоков", 3),
                QuizQuestion(questionId++, 2, 5, "Как работает HashMap?", "Хеш-таблица для ключ-значение", 2),
                QuizQuestion(questionId++, 2, 5, "Что такое generics?", "Обобщенные типы", 3)
            )
            qql.addAll(javaBasicQuestions)

            // Java - ООП в Java (15 вопросов)
            val javaOopQuestions = listOf(
                QuizQuestion(questionId++, 2, 6, "Что такое инкапсуляция?", "Сокрытие реализации", 2),
                QuizQuestion(questionId++, 2, 6, "Как работает наследование?", "extends keyword", 2),
                QuizQuestion(questionId++, 2, 6, "Что такое полиморфизм?", "Разные реализации методов", 3),
                QuizQuestion(questionId++, 2, 6, "Как создать абстрактный класс?", "abstract class", 2),
                QuizQuestion(questionId++, 2, 6, "Что такое интерфейс?", "Контракт без реализации", 2),
                QuizQuestion(questionId++, 2, 6, "Как работает super()?", "Вызов конструктора родителя", 2),
                QuizQuestion(questionId++, 2, 6, "Что такое конструктор?", "Метод инициализации объекта", 1),
                QuizQuestion(questionId++, 2, 6, "Как перегрузить метод?", "Метод с тем же именем но разными параметрами", 2),
                QuizQuestion(questionId++, 2, 6, "Что такое переопределение?", "Изменение метода родителя в потомке", 2),
                QuizQuestion(questionId++, 2, 6, "Как создать внутренний класс?", "Class внутри класса", 3),
                QuizQuestion(questionId++, 2, 6, "Что такое статический класс?", "Вложенный static класс", 3),
                QuizQuestion(questionId++, 2, 6, "Как работает final с классами?", "Запрещает наследование", 2),
                QuizQuestion(questionId++, 2, 6, "Что такое абстрактный метод?", "Метод без реализации", 2),
                QuizQuestion(questionId++, 2, 6, "Как создать singleton?", "private конструктор, static instance", 3),
                QuizQuestion(questionId++, 2, 6, "Что такое factory pattern?", "Паттерн создания объектов", 3)
            )
            qql.addAll(javaOopQuestions)

            // JavaScript - Входной тест (15 вопросов)
            val jsInputQuestions = listOf(
                QuizQuestion(questionId++, 3, 7, "Как объявить переменную?", "let, const, var", 1),
                QuizQuestion(questionId++, 3, 7, "Что выведет console.log(2 + '2')?", "Конкатенация строк", 2),
                QuizQuestion(questionId++, 3, 7, "Как создать массив?", "[] или new Array()", 1),
                QuizQuestion(questionId++, 3, 7, "Что такое typeof?", "Оператор проверки типа", 1),
                QuizQuestion(questionId++, 3, 7, "Как объявить функцию?", "function name() или const name = () =>", 1),
                QuizQuestion(questionId++, 3, 7, "Что такое DOM?", "Document Object Model", 1),
                QuizQuestion(questionId++, 3, 7, "Как выбрать элемент по id?", "document.getElementById()", 1),
                QuizQuestion(questionId++, 3, 7, "Что такое event listener?", "Обработчик событий", 2),
                QuizQuestion(questionId++, 3, 7, "Как работает ===?", "Строгое сравнение без приведения типов", 2),
                QuizQuestion(questionId++, 3, 7, "Что такое JSON?", "JavaScript Object Notation", 1),
                QuizQuestion(questionId++, 3, 7, "Как преобразовать в строку?", "JSON.stringify() или toString()", 2),
                QuizQuestion(questionId++, 3, 7, "Что такое closure?", "Замыкание - функция с запомненным окружением", 3),
                QuizQuestion(questionId++, 3, 7, "Как работает setTimeout?", "Выполняет функцию после задержки", 2),
                QuizQuestion(questionId++, 3, 7, "Что такое this?", "Контекст выполнения", 2),
                QuizQuestion(questionId++, 3, 7, "Как создать объект?", "{} или new Object()", 1)
            )
            qql.addAll(jsInputQuestions)

            // JavaScript - Основы JavaScript (15 вопросов)
            val jsBasicQuestions = listOf(
                QuizQuestion(questionId++, 3, 8, "Что такое hoisting?", "Поднятие переменных и функций", 2),
                QuizQuestion(questionId++, 3, 8, "Как работает let vs var?", "Блочная vs функциональная область видимости", 2),
                QuizQuestion(questionId++, 3, 8, "Что такое arrow function?", "Стрелочные функции", 2),
                QuizQuestion(questionId++, 3, 8, "Как работает destructuring?", "Разбор объектов и массивов", 2),
                QuizQuestion(questionId++, 3, 8, "Что такое template literals?", "Строки с для переменных", 1),
                QuizQuestion(questionId++, 3, 8, "Как работает spread operator?", "... для копирования и объединения", 2),
                QuizQuestion(questionId++, 3, 8, "Что такое rest parameters?", "... для аргументов функции", 2),
                QuizQuestion(questionId++, 3, 8, "Как создать класс?", "class MyClass {}", 2),
                QuizQuestion(questionId++, 3, 8, "Что такое prototype?", "Механизм наследования в JS", 3),
                QuizQuestion(questionId++, 3, 8, "Как работает inheritance?", "extends keyword", 2),
                QuizQuestion(questionId++, 3, 8, "Что такое callback?", "Функция передаваемая как аргумент", 2),
                QuizQuestion(questionId++, 3, 8, "Как работает map()?", "Преобразование элементов массива", 2),
                QuizQuestion(questionId++, 3, 8, "Что такое filter()?", "Фильтрация элементов массива", 2),
                QuizQuestion(questionId++, 3, 8, "Как работает reduce()?", "Агрегация элементов массива", 3),
                QuizQuestion(questionId++, 3, 8, "Что такое Promise?", "Обещание будущего результата", 2)
            )
            qql.addAll(jsBasicQuestions)

            // JavaScript - Асинхронность в JS (15 вопросов)
            val jsAsyncQuestions = listOf(
                QuizQuestion(questionId++, 3, 9, "Что такое Promise?", "Обещание будущего значения", 2),
                QuizQuestion(questionId++, 3, 9, "Как создать Promise?", "new Promise((resolve, reject) => {})", 2),
                QuizQuestion(questionId++, 3, 9, "Что такое then/catch?", "Обработка успеха и ошибок Promise", 2),
                QuizQuestion(questionId++, 3, 9, "Как работает async/await?", "Синтаксический сахар для Promise", 2),
                QuizQuestion(questionId++, 3, 9, "Что такое Promise.all?", "Ожидание всех Promise", 2),
                QuizQuestion(questionId++, 3, 9, "Как работает Promise.race?", "Ожидание первого завершенного Promise", 2),
                QuizQuestion(questionId++, 3, 9, "Что такое Promise.allSettled?", "Ожидание всех Promise с результатами", 3),
                QuizQuestion(questionId++, 3, 9, "Как работает Promise.any?", "Ожидание первого успешного Promise", 3),
                QuizQuestion(questionId++, 3, 9, "Что такое microtask queue?", "Очередь микрозадач", 3),
                QuizQuestion(questionId++, 3, 9, "Как работает event loop?", "Цикл событий", 3),
                QuizQuestion(questionId++, 3, 9, "Что такое callback queue?", "Очередь колбэков", 3),
                QuizQuestion(questionId++, 3, 9, "Как работает setTimeout?", "Макрозадача с задержкой", 2),
                QuizQuestion(questionId++, 3, 9, "Что такое setImmediate?", "Макрозадача без задержки (Node.js)", 3),
                QuizQuestion(questionId++, 3, 9, "Как работает process.nextTick?", "Микрозадача в Node.js", 3),
                QuizQuestion(questionId++, 3, 9, "Что такое async iterator?", "Итератор для асинхронных данных", 3)
            )
            qql.addAll(jsAsyncQuestions)

            quizQuestionRepo.insertAllQuizQuestions(qql)
        }

        if(!flo) {
            val lo = mutableListOf<Option>()
            var optionId = 1

            // Python входной тест (вопросы 1-15)
            // 1. Что выведет print(2 ** 3)?
            lo.add(Option(optionId++, 1, "8", true))
            lo.add(Option(optionId++, 1, "6", false))
            lo.add(Option(optionId++, 1, "9", false))
            lo.add(Option(optionId++, 1, "Ошибку", false))

            // 2. Какой тип данных у значения 3.14?
            lo.add(Option(optionId++, 2, "float", true))
            lo.add(Option(optionId++, 2, "int", false))
            lo.add(Option(optionId++, 2, "double", false))
            lo.add(Option(optionId++, 2, "decimal", false))

            // 3. Как создать пустой список?
            lo.add(Option(optionId++, 3, "[]", true))
            lo.add(Option(optionId++, 3, "list()", true))
            lo.add(Option(optionId++, 3, "{}", false))
            lo.add(Option(optionId++, 3, "()", false))

            // 4. Что делает метод append()?
            lo.add(Option(optionId++, 4, "Добавляет элемент в конец списка", true))
            lo.add(Option(optionId++, 4, "Удаляет элемент из списка", false))
            lo.add(Option(optionId++, 4, "Сортирует список", false))
            lo.add(Option(optionId++, 4, "Объединяет два списка", false))

            // 5. Как получить длину строки?
            lo.add(Option(optionId++, 5, "len()", true))
            lo.add(Option(optionId++, 5, "length()", false))
            lo.add(Option(optionId++, 5, "size()", false))
            lo.add(Option(optionId++, 5, "count()", false))

            // 6. Что вернет 'hello'[1:4]?
            lo.add(Option(optionId++, 6, "'ell'", true))
            lo.add(Option(optionId++, 6, "'hel'", false))
            lo.add(Option(optionId++, 6, "'ello'", false))
            lo.add(Option(optionId++, 6, "'hell'", false))

            // 7. Как объявить функцию?
            lo.add(Option(optionId++, 7, "def my_func():", true))
            lo.add(Option(optionId++, 7, "function my_func()", false))
            lo.add(Option(optionId++, 7, "func my_func():", false))
            lo.add(Option(optionId++, 7, "define my_func():", false))

            // 8. Что такое None?
            lo.add(Option(optionId++, 8, "Объект, обозначающий отсутствие значения", true))
            lo.add(Option(optionId++, 8, "Пустая строка", false))
            lo.add(Option(optionId++, 8, "Число 0", false))
            lo.add(Option(optionId++, 8, "Ложное значение", false))

            // 9. Как импортировать модуль?
            lo.add(Option(optionId++, 9, "import module", true))
            lo.add(Option(optionId++, 9, "include module", false))
            lo.add(Option(optionId++, 9, "require module", false))
            lo.add(Option(optionId++, 9, "using module", false))

            // 10. Что делает range(5)?
            lo.add(Option(optionId++, 10, "Создает последовательность 0,1,2,3,4", true))
            lo.add(Option(optionId++, 10, "Создает последовательность 1,2,3,4,5", false))
            lo.add(Option(optionId++, 10, "Создает список из 5 нулей", false))
            lo.add(Option(optionId++, 10, "Генерирует случайное число до 5", false))

            // 11. Как работает оператор in?
            lo.add(Option(optionId++, 11, "Проверяет наличие элемента в последовательности", true))
            lo.add(Option(optionId++, 11, "Проверяет тип переменной", false))
            lo.add(Option(optionId++, 11, "Импортирует модуль", false))
            lo.add(Option(optionId++, 11, "Создает цикл", false))

            // 12. Что такое tuple?
            lo.add(Option(optionId++, 12, "Неизменяемая последовательность", true))
            lo.add(Option(optionId++, 12, "Изменяемая последовательность", false))
            lo.add(Option(optionId++, 12, "Словарь с ключами", false))
            lo.add(Option(optionId++, 12, "Специальный тип функции", false))

            // 13. Как создать словарь?
            lo.add(Option(optionId++, 13, "{}", true))
            lo.add(Option(optionId++, 13, "dict()", true))
            lo.add(Option(optionId++, 13, "[]", false))
            lo.add(Option(optionId++, 13, "()", false))

            // 14. Что делает метод get() у словаря?
            lo.add(Option(optionId++, 14, "Возвращает значение по ключу или None", true))
            lo.add(Option(optionId++, 14, "Добавляет новый ключ", false))
            lo.add(Option(optionId++, 14, "Удаляет ключ", false))
            lo.add(Option(optionId++, 14, "Проверяет наличие ключа", false))

            // 15. Как обработать исключение?
            lo.add(Option(optionId++, 15, "try: ... except: ...", true))
            lo.add(Option(optionId++, 15, "catch: ... finally: ...", false))
            lo.add(Option(optionId++, 15, "error: ... handle: ...", false))
            lo.add(Option(optionId++, 15, "exception: ... resolve: ...", false))

            // Python основы (вопросы 16-30)
            // 16. Что выведет: print([i for i in range(5) if i % 2 == 0])?
            lo.add(Option(optionId++, 16, "[0, 2, 4]", true))
            lo.add(Option(optionId++, 16, "[1, 3, 5]", false))
            lo.add(Option(optionId++, 16, "[0, 1, 2, 3, 4]", false))
            lo.add(Option(optionId++, 16, "[2, 4]", false))

            // 17. Какой результат: 'abc' * 3?
            lo.add(Option(optionId++, 17, "'abcabcabc'", true))
            lo.add(Option(optionId++, 17, "'abc3'", false))
            lo.add(Option(optionId++, 17, "'aaa bbb ccc'", false))
            lo.add(Option(optionId++, 17, "Ошибку", false))

            // 18. Что вернет: bool('False')?
            lo.add(Option(optionId++, 18, "True", true))
            lo.add(Option(optionId++, 18, "False", false))
            lo.add(Option(optionId++, 18, "None", false))
            lo.add(Option(optionId++, 18, "Ошибку", false))

            // 19. Как работает метод strip()?
            lo.add(Option(optionId++, 19, "Удаляет пробелы с обоих концов строки", true))
            lo.add(Option(optionId++, 19, "Разбивает строку на список", false))
            lo.add(Option(optionId++, 19, "Заменяет пробелы на табы", false))
            lo.add(Option(optionId++, 19, "Удаляет все пробелы из строки", false))

            // 20. Что делает оператор //?
            lo.add(Option(optionId++, 20, "Целочисленное деление", true))
            lo.add(Option(optionId++, 20, "Комментарий", false))
            lo.add(Option(optionId++, 20, "Деление с остатком", false))
            lo.add(Option(optionId++, 20, "Возведение в степень", false))

            // 21. Как отсортировать список по убыванию?
            lo.add(Option(optionId++, 21, "sorted(list, reverse=True)", true))
            lo.add(Option(optionId++, 21, "list.sort(reverse=True)", true))
            lo.add(Option(optionId++, 21, "list.descend()", false))
            lo.add(Option(optionId++, 21, "sorted(list, order='desc')", false))

            // 22. Что такое lambda-функция?
            lo.add(Option(optionId++, 22, "Анонимная функция", true))
            lo.add(Option(optionId++, 22, "Функция из модуля lambda", false))
            lo.add(Option(optionId++, 22, "Рекурсивная функция", false))
            lo.add(Option(optionId++, 22, "Генераторная функция", false))

            // 23. Как объединить два словаря?
            lo.add(Option(optionId++, 23, "dict1.update(dict2)", true))
            lo.add(Option(optionId++, 23, "dict1 + dict2", false))
            lo.add(Option(optionId++, 23, "dict1.merge(dict2)", false))
            lo.add(Option(optionId++, 23, "concat(dict1, dict2)", false))

            // 24. Что делает метод join()?
            lo.add(Option(optionId++, 24, "Объединяет строки из списка в одну строку", true))
            lo.add(Option(optionId++, 24, "Разбивает строку на список", false))
            lo.add(Option(optionId++, 24, "Добавляет элемент в список", false))
            lo.add(Option(optionId++, 24, "Соединяет два списка", false))

            // 25. Как создать множество?
            lo.add(Option(optionId++, 25, "{1, 2, 3}", true))
            lo.add(Option(optionId++, 25, "set([1, 2, 3])", true))
            lo.add(Option(optionId++, 25, "[1, 2, 3]", false))
            lo.add(Option(optionId++, 25, "(1, 2, 3)", false))

            // 26. Что такое frozenset?
            lo.add(Option(optionId++, 26, "Неизменяемое множество", true))
            lo.add(Option(optionId++, 26, "Замороженный список", false))
            lo.add(Option(optionId++, 26, "Словарь с frozen ключами", false))
            lo.add(Option(optionId++, 26, "Специальный тип кортежа", false))

            // 27. Как работает оператор is?
            lo.add(Option(optionId++, 27, "Проверяет идентичность объектов", true))
            lo.add(Option(optionId++, 27, "Проверяет равенство значений", false))
            lo.add(Option(optionId++, 27, "Проверяет тип объекта", false))
            lo.add(Option(optionId++, 27, "Проверяет наличие атрибута", false))

            // 28. Что такое генератор?
            lo.add(Option(optionId++, 28, "Функция с yield вместо return", true))
            lo.add(Option(optionId++, 28, "Функция, которая генерирует числа", false))
            lo.add(Option(optionId++, 28, "Модуль для генерации данных", false))
            lo.add(Option(optionId++, 28, "Класс для создания объектов", false))

            // 29. Как сделать глубокую копию списка?
            lo.add(Option(optionId++, 29, "copy.deepcopy()", true))
            lo.add(Option(optionId++, 29, "list.copy()", false))
            lo.add(Option(optionId++, 29, "list[:]", false))
            lo.add(Option(optionId++, 29, "list.clone()", false))

            // 30. Что делает функция enumerate()?
            lo.add(Option(optionId++, 30, "Возвращает индекс и значение элементов", true))
            lo.add(Option(optionId++, 30, "Считает количество элементов", false))
            lo.add(Option(optionId++, 30, "Перечисляет атрибуты объекта", false))
            lo.add(Option(optionId++, 30, "Создает нумерованный список", false))

            // Python ООП (вопросы 31-45)
            // 31. Как объявить класс в Python?
            lo.add(Option(optionId++, 31, "class MyClass:", true))
            lo.add(Option(optionId++, 31, "def class MyClass:", false))
            lo.add(Option(optionId++, 31, "MyClass class {}", false))
            lo.add(Option(optionId++, 31, "class MyClass {}", false))

            // 32. Что такое self в методах класса?
            lo.add(Option(optionId++, 32, "Ссылка на экземпляр класса", true))
            lo.add(Option(optionId++, 32, "Ссылка на класс", false))
            lo.add(Option(optionId++, 32, "Зарезервированное ключевое слово", false))
            lo.add(Option(optionId++, 32, "Ссылка на родительский класс", false))

            // 33. Как создать приватный атрибут?
            lo.add(Option(optionId++, 33, "__attr", true))
            lo.add(Option(optionId++, 33, "_attr", false))
            lo.add(Option(optionId++, 33, "private attr", false))
            lo.add(Option(optionId++, 33, "#attr", false))

            // 34. Что такое наследование?
            lo.add(Option(optionId++, 34, "Создание класса на основе существующего", true))
            lo.add(Option(optionId++, 34, "Копирование методов класса", false))
            lo.add(Option(optionId++, 34, "Импорт классов из модулей", false))
            lo.add(Option(optionId++, 34, "Создание экземпляров класса", false))

            // 35. Как вызвать метод родительского класса?
            lo.add(Option(optionId++, 35, "super().method()", true))
            lo.add(Option(optionId++, 35, "parent().method()", false))
            lo.add(Option(optionId++, 35, "ParentClass.method()", false))
            lo.add(Option(optionId++, 35, "this().method()", false))

            // 36. Что такое полиморфизм?
            lo.add(Option(optionId++, 36, "Разное поведение методов с одним именем", true))
            lo.add(Option(optionId++, 36, "Множественное наследование", false))
            lo.add(Option(optionId++, 36, "Сокрытие реализации", false))
            lo.add(Option(optionId++, 36, "Создание абстракций", false))

            // 37. Как создать абстрактный класс?
            lo.add(Option(optionId++, 37, "from abc import ABC, abstractmethod", true))
            lo.add(Option(optionId++, 37, "abstract class MyClass", false))
            lo.add(Option(optionId++, 37, "class abstract MyClass", false))
            lo.add(Option(optionId++, 37, "class MyClass(Abstract)", false))

            // 38. Что такое метод __init__?
            lo.add(Option(optionId++, 38, "Конструктор класса", true))
            lo.add(Option(optionId++, 38, "Деструктор класса", false))
            lo.add(Option(optionId++, 38, "Инициализатор модуля", false))
            lo.add(Option(optionId++, 38, "Статический метод", false))

            // 39. Как работает __str__?
            lo.add(Option(optionId++, 39, "Возвращает строковое представление объекта", true))
            lo.add(Option(optionId++, 39, "Преобразует объект в строку", false))
            lo.add(Option(optionId++, 39, "Выводит объект на печать", false))
            lo.add(Option(optionId++, 39, "Сравнивает объекты как строки", false))

            // 40. Что такое property?
            lo.add(Option(optionId++, 40, "Свойство класса с getter/setter", true))
            lo.add(Option(optionId++, 40, "Атрибут класса", false))
            lo.add(Option(optionId++, 40, "Метод класса", false))
            lo.add(Option(optionId++, 40, "Декоратор для функций", false))

            // 41. Как создать статический метод?
            lo.add(Option(optionId++, 41, "@staticmethod", true))
            lo.add(Option(optionId++, 41, "static method", false))
            lo.add(Option(optionId++, 41, "@static", false))
            lo.add(Option(optionId++, 41, "def static method", false))

            // 42. Что такое классовый метод?
            lo.add(Option(optionId++, 42, "Метод с параметром cls", true))
            lo.add(Option(optionId++, 42, "Метод без параметров", false))
            lo.add(Option(optionId++, 42, "Метод только для класса", false))
            lo.add(Option(optionId++, 42, "Статический метод", false))

            // 43. Как работает множественное наследование?
            lo.add(Option(optionId++, 43, "Класс наследует от нескольких классов", true))
            lo.add(Option(optionId++, 43, "Создание нескольких экземпляров", false))
            lo.add(Option(optionId++, 43, "Наследование от одного класса несколько раз", false))
            lo.add(Option(optionId++, 43, "Импорт нескольких модулей", false))

            // 44. Что такое MRO?
            lo.add(Option(optionId++, 44, "Method Resolution Order", true))
            lo.add(Option(optionId++, 44, "Multiple Return Object", false))
            lo.add(Option(optionId++, 44, "Method Reference Order", false))
            lo.add(Option(optionId++, 44, "Module Resolution Order", false))

            // 45. Как создать перечисление?
            lo.add(Option(optionId++, 45, "from enum import Enum", true))
            lo.add(Option(optionId++, 45, "enum MyEnum", false))
            lo.add(Option(optionId++, 45, "class enum MyEnum", false))
            lo.add(Option(optionId++, 45, "import Enum", false))

            // Java входной тест (вопросы 46-60)
            // 46. Как объявить переменную в Java?
            lo.add(Option(optionId++, 46, "int x = 5;", true))
            lo.add(Option(optionId++, 46, "var x = 5;", false))
            lo.add(Option(optionId++, 46, "x := 5;", false))
            lo.add(Option(optionId++, 46, "let x = 5;", false))

            // 47. Что такое main метод?
            lo.add(Option(optionId++, 47, "Точка входа в программу", true))
            lo.add(Option(optionId++, 47, "Метод для вывода текста", false))
            lo.add(Option(optionId++, 47, "Конструктор класса", false))
            lo.add(Option(optionId++, 47, "Статический блок инициализации", false))

            // 48. Как создать массив?
            lo.add(Option(optionId++, 48, "int[] arr = new int[5];", true))
            lo.add(Option(optionId++, 48, "int arr[] = [1,2,3];", false))
            lo.add(Option(optionId++, 48, "array int[5];", false))
            lo.add(Option(optionId++, 48, "new Array(5);", false))

            // 49. Что выведет System.out.println(5 + "5")?
            lo.add(Option(optionId++, 49, "55", true))
            lo.add(Option(optionId++, 49, "10", false))
            lo.add(Option(optionId++, 49, "5", false))
            lo.add(Option(optionId++, 49, "Ошибку компиляции", false))

            // 50. Как объявить класс?
            lo.add(Option(optionId++, 50, "public class MyClass {}", true))
            lo.add(Option(optionId++, 50, "class MyClass {}", true))
            lo.add(Option(optionId++, 50, "MyClass class {}", false))
            lo.add(Option(optionId++, 50, "def class MyClass:", false))

            // 51. Что такое package?
            lo.add(Option(optionId++, 51, "Пространство имен для классов", true))
            lo.add(Option(optionId++, 51, "Файл с кодом", false))
            lo.add(Option(optionId++, 51, "Архив библиотек", false))
            lo.add(Option(optionId++, 51, "Тип данных", false))

            // 52. Как работает цикл for?
            lo.add(Option(optionId++, 52, "for(инициализация; условие; инкремент)", true))
            lo.add(Option(optionId++, 52, "for i in range()", false))
            lo.add(Option(optionId++, 52, "for each in collection", false))
            lo.add(Option(optionId++, 52, "for(элемент : коллекция)", false))

            // 53. Что такое ArrayList?
            lo.add(Option(optionId++, 53, "Динамический массив", true))
            lo.add(Option(optionId++, 53, "Статический массив", false))
            lo.add(Option(optionId++, 53, "Список массивов", false))
            lo.add(Option(optionId++, 53, "Интерфейс для массивов", false))

            // 54. Как сравнить строки?
            lo.add(Option(optionId++, 54, "str1.equals(str2)", true))
            lo.add(Option(optionId++, 54, "str1 == str2", false))
            lo.add(Option(optionId++, 54, "str1 === str2", false))
            lo.add(Option(optionId++, 54, "str1.compare(str2)", false))

            // 55. Что такое final?
            lo.add(Option(optionId++, 55, "Константа или запрет наследования", true))
            lo.add(Option(optionId++, 55, "Конечный метод", false))
            lo.add(Option(optionId++, 55, "Тип переменной", false))
            lo.add(Option(optionId++, 55, "Модификатор доступа", false))

            // 56. Как обработать исключение?
            lo.add(Option(optionId++, 56, "try-catch блок", true))
            lo.add(Option(optionId++, 56, "try-except блок", false))
            lo.add(Option(optionId++, 56, "error-handler блок", false))
            lo.add(Option(optionId++, 56, "exception блок", false))

            // 57. Что такое interface?
            lo.add(Option(optionId++, 57, "Контракт без реализации", true))
            lo.add(Option(optionId++, 57, "Абстрактный класс", false))
            lo.add(Option(optionId++, 57, "Тип данных", false))
            lo.add(Option(optionId++, 57, "Метод доступа", false))

            // 58. Как создать объект?
            lo.add(Option(optionId++, 58, "new MyClass()", true))
            lo.add(Option(optionId++, 58, "MyClass.create()", false))
            lo.add(Option(optionId++, 58, "new object MyClass", false))
            lo.add(Option(optionId++, 58, "MyClass()", false))

            // 59. Что такое static?
            lo.add(Option(optionId++, 59, "Принадлежит классу, а не экземпляру", true))
            lo.add(Option(optionId++, 59, "Неизменяемый", false))
            lo.add(Option(optionId++, 59, "Синхронизированный", false))
            lo.add(Option(optionId++, 59, "Финальный", false))

            // 60. Как наследовать класс?
            lo.add(Option(optionId++, 60, "extends", true))
            lo.add(Option(optionId++, 60, "implements", false))
            lo.add(Option(optionId++, 60, "inherits", false))
            lo.add(Option(optionId++, 60, "super", false))

            // Java основы (вопросы 61-75)
            // 61. Что такое JVM?
            lo.add(Option(optionId++, 61, "Виртуальная машина Java", true))
            lo.add(Option(optionId++, 61, "Java Variable Manager", false))
            lo.add(Option(optionId++, 61, "Java Version Manager", false))
            lo.add(Option(optionId++, 61, "Java Virtual Module", false))

            // 62. Как работает garbage collection?
            lo.add(Option(optionId++, 62, "Автоматическое освобождение памяти", true))
            lo.add(Option(optionId++, 62, "Ручное удаление объектов", false))
            lo.add(Option(optionId++, 62, "Сбор мусора в коде", false))
            lo.add(Option(optionId++, 62, "Очистка кэша", false))

            // 63. Что такое bytecode?
            lo.add(Option(optionId++, 63, "Промежуточный код для JVM", true))
            lo.add(Option(optionId++, 63, "Двоичный код процессора", false))
            lo.add(Option(optionId++, 63, "Код в байтах", false))
            lo.add(Option(optionId++, 63, "Сжатый код", false))

            // 64. Как объявить константу?
            lo.add(Option(optionId++, 64, "static final", true))
            lo.add(Option(optionId++, 64, "const", false))
            lo.add(Option(optionId++, 64, "final", false))
            lo.add(Option(optionId++, 64, "constant", false))

            // 65. Что такое autoboxing?
            lo.add(Option(optionId++, 65, "Автоматическая упаковка примитивов", true))
            lo.add(Option(optionId++, 65, "Упаковка объектов в коробки", false))
            lo.add(Option(optionId++, 65, "Создание боксов для UI", false))
            lo.add(Option(optionId++, 65, "Компактное хранение данных", false))

            // 66. Как работает switch с строками?
            lo.add(Option(optionId++, 66, "С Java 7 можно использовать String", true))
            lo.add(Option(optionId++, 66, "Только с Java 11", false))
            lo.add(Option(optionId++, 66, "Не поддерживается", false))
            lo.add(Option(optionId++, 66, "Только с enum", false))

            // 67. Что такое varargs?
            lo.add(Option(optionId++, 67, "Аргументы переменной длины", true))
            lo.add(Option(optionId++, 67, "Вариативные типы", false))
            lo.add(Option(optionId++, 67, "Переменные аргументы", false))
            lo.add(Option(optionId++, 67, "Варьируемые параметры", false))

            // 68. Как создать immutable класс?
            lo.add(Option(optionId++, 68, "final класс, final поля, нет сеттеров", true))
            lo.add(Option(optionId++, 68, "class с модификатором immutable", false))
            lo.add(Option(optionId++, 68, "использовать ключевое слово const", false))
            lo.add(Option(optionId++, 68, "только приватные конструкторы", false))

            // 69. Что такое enum?
            lo.add(Option(optionId++, 69, "Перечисление констант", true))
            lo.add(Option(optionId++, 69, "Тип данных для чисел", false))
            lo.add(Option(optionId++, 69, "Интерфейс перечисления", false))
            lo.add(Option(optionId++, 69, "Метод для сравнения", false))

            // 70. Как работает try-with-resources?
            lo.add(Option(optionId++, 70, "Автоматическое закрытие ресурсов", true))
            lo.add(Option(optionId++, 70, "Обработка ресурсов в try", false))
            lo.add(Option(optionId++, 70, "Создание ресурсов в блоке", false))
            lo.add(Option(optionId++, 70, "Освобождение памяти", false))

            // 71. Что такое аннотации?
            lo.add(Option(optionId++, 71, "Метаданные для кода", true))
            lo.add(Option(optionId++, 71, "Комментарии в коде", false))
            lo.add(Option(optionId++, 71, "Типы данных", false))
            lo.add(Option(optionId++, 71, "Модификаторы доступа", false))

            // 72. Как создать поток?
            lo.add(Option(optionId++, 72, "Thread класс или Runnable", true))
            lo.add(Option(optionId++, 72, "new Process()", false))
            lo.add(Option(optionId++, 72, "Thread.create()", false))
            lo.add(Option(optionId++, 72, "Runnable.run()", false))

            // 73. Что такое synchronized?
            lo.add(Option(optionId++, 73, "Синхронизация потоков", true))
            lo.add(Option(optionId++, 73, "Синхронизация данных", false))
            lo.add(Option(optionId++, 73, "Блокировка класса", false))
            lo.add(Option(optionId++, 73, "Модификатор метода", false))

            // 74. Как работает HashMap?
            lo.add(Option(optionId++, 74, "Хеш-таблица для ключ-значение", true))
            lo.add(Option(optionId++, 74, "Отсортированная карта", false))
            lo.add(Option(optionId++, 74, "Список пар ключ-значение", false))
            lo.add(Option(optionId++, 74, "Древовидная структура", false))

            // 75. Что такое generics?
            lo.add(Option(optionId++, 75, "Обобщенные типы", true))
            lo.add(Option(optionId++, 75, "Генерация кода", false))
            lo.add(Option(optionId++, 75, "Общие методы", false))
            lo.add(Option(optionId++, 75, "Универсальные классы", false))

            // Java ООП (вопросы 76-90)
            // 76. Что такое инкапсуляция?
            lo.add(Option(optionId++, 76, "Сокрытие реализации", true))
            lo.add(Option(optionId++, 76, "Объединение данных и методов", true))
            lo.add(Option(optionId++, 76, "Наследование свойств", false))
            lo.add(Option(optionId++, 76, "Полиморфизм методов", false))

            // 77. Как работает наследование?
            lo.add(Option(optionId++, 77, "extends keyword", true))
            lo.add(Option(optionId++, 77, "implements keyword", false))
            lo.add(Option(optionId++, 77, "inherits keyword", false))
            lo.add(Option(optionId++, 77, "super keyword", false))

            // 78. Что такое полиморфизм?
            lo.add(Option(optionId++, 78, "Разные реализации методов", true))
            lo.add(Option(optionId++, 78, "Множественное наследование", false))
            lo.add(Option(optionId++, 78, "Перегрузка операторов", false))
            lo.add(Option(optionId++, 78, "Динамическое связывание", false))

            // 79. Как создать абстрактный класс?
            lo.add(Option(optionId++, 79, "abstract class", true))
            lo.add(Option(optionId++, 79, "class abstract", false))
            lo.add(Option(optionId++, 79, "abstract interface", false))
            lo.add(Option(optionId++, 79, "interface abstract", false))

            // 80. Что такое интерфейс?
            lo.add(Option(optionId++, 80, "Контракт без реализации", true))
            lo.add(Option(optionId++, 80, "Абстрактный класс", false))
            lo.add(Option(optionId++, 80, "Класс с методами по умолчанию", false))
            lo.add(Option(optionId++, 80, "Тип данных", false))

            // 81. Как работает super()?
            lo.add(Option(optionId++, 81, "Вызов конструктора родителя", true))
            lo.add(Option(optionId++, 81, "Вызов метода родителя", false))
            lo.add(Option(optionId++, 81, "Создание суперкласса", false))
            lo.add(Option(optionId++, 81, "Доступ к родительским полям", false))

            // 82. Что такое конструктор?
            lo.add(Option(optionId++, 82, "Метод инициализации объекта", true))
            lo.add(Option(optionId++, 82, "Специальный класс", false))
            lo.add(Option(optionId++, 82, "Статический метод", false))
            lo.add(Option(optionId++, 82, "Финальный метод", false))

            // 83. Как перегрузить метод?
            lo.add(Option(optionId++, 83, "Метод с тем же именем но разными параметрами", true))
            lo.add(Option(optionId++, 83, "Изменить реализацию метода", false))
            lo.add(Option(optionId++, 83, "Создать метод с другим именем", false))
            lo.add(Option(optionId++, 83, "Использовать аннотацию @Override", false))

            // 84. Что такое переопределение?
            lo.add(Option(optionId++, 84, "Изменение метода родителя в потомке", true))
            lo.add(Option(optionId++, 84, "Создание нового метода", false))
            lo.add(Option(optionId++, 84, "Перегрузка метода", false))
            lo.add(Option(optionId++, 84, "Изменение сигнатуры метода", false))

            // 85. Как создать внутренний класс?
            lo.add(Option(optionId++, 85, "Class внутри класса", true))
            lo.add(Option(optionId++, 85, "Class с модификатором inner", false))
            lo.add(Option(optionId++, 85, "Class в отдельном файле", false))
            lo.add(Option(optionId++, 85, "Class с модификатором internal", false))

            // 86. Что такое статический класс?
            lo.add(Option(optionId++, 86, "Вложенный static класс", true))
            lo.add(Option(optionId++, 86, "Класс с static методами", false))
            lo.add(Option(optionId++, 86, "Класс в static контексте", false))
            lo.add(Option(optionId++, 86, "Абстрактный класс", false))

            // 87. Как работает final с классами?
            lo.add(Option(optionId++, 87, "Запрещает наследование", true))
            lo.add(Option(optionId++, 87, "Делает класс неизменяемым", false))
            lo.add(Option(optionId++, 87, "Запрещает создание экземпляров", false))
            lo.add(Option(optionId++, 87, "Делает все методы final", false))

            // 88. Что такое абстрактный метод?
            lo.add(Option(optionId++, 88, "Метод без реализации", true))
            lo.add(Option(optionId++, 88, "Метод с реализацией", false))
            lo.add(Option(optionId++, 88, "Статический метод", false))
            lo.add(Option(optionId++, 88, "Финальный метод", false))

            // 89. Как создать singleton?
            lo.add(Option(optionId++, 89, "private конструктор, static instance", true))
            lo.add(Option(optionId++, 89, "public конструктор, static instance", false))
            lo.add(Option(optionId++, 89, "только private методы", false))
            lo.add(Option(optionId++, 89, "использовать ключевое слово singleton", false))

            // 90. Что такое factory pattern?
            lo.add(Option(optionId++, 90, "Паттерн создания объектов", true))
            lo.add(Option(optionId++, 90, "Паттерн для фабрик", false))
            lo.add(Option(optionId++, 90, "Шаблон для production", false))
            lo.add(Option(optionId++, 90, "Метод создания классов", false))

            // JavaScript входной тест (вопросы 91-105)
            // 91. Как объявить переменную?
            lo.add(Option(optionId++, 91, "let x = 5;", true))
            lo.add(Option(optionId++, 91, "const x = 5;", true))
            lo.add(Option(optionId++, 91, "var x = 5;", true))
            lo.add(Option(optionId++, 91, "int x = 5;", false))

            // 92. Что выведет console.log(2 + '2')?
            lo.add(Option(optionId++, 92, "22", true))
            lo.add(Option(optionId++, 92, "4", false))
            lo.add(Option(optionId++, 92, "NaN", false))
            lo.add(Option(optionId++, 92, "Ошибку", false))

            // 93. Как создать массив?
            lo.add(Option(optionId++, 93, "[]", true))
            lo.add(Option(optionId++, 93, "new Array()", true))
            lo.add(Option(optionId++, 93, "array()", false))
            lo.add(Option(optionId++, 93, "List()", false))

            // 94. Что такое typeof?
            lo.add(Option(optionId++, 94, "Оператор для проверки типа данных", true))
            lo.add(Option(optionId++, 94, "Функция для преобразования типов", false))
            lo.add(Option(optionId++, 94, "Метод для создания объектов", false))
            lo.add(Option(optionId++, 94, "Ключевое слово для объявления типа", false))

            // 95. Как объявить функцию?
            lo.add(Option(optionId++, 95, "function myFunc() {}", true))
            lo.add(Option(optionId++, 95, "const myFunc = () => {}", true))
            lo.add(Option(optionId++, 95, "def myFunc():", false))
            lo.add(Option(optionId++, 95, "func myFunc()", false))

            // 96. Что такое DOM?
            lo.add(Option(optionId++, 96, "Document Object Model", true))
            lo.add(Option(optionId++, 96, "Data Object Model", false))
            lo.add(Option(optionId++, 96, "Document Orientation Model", false))
            lo.add(Option(optionId++, 96, "Digital Object Management", false))

            // 97. Как выбрать элемент по id?
            lo.add(Option(optionId++, 97, "document.getElementById()", true))
            lo.add(Option(optionId++, 97, "document.querySelector()", true))
            lo.add(Option(optionId++, 97, "document.getElement()", false))
            lo.add(Option(optionId++, 97, "document.findElement()", false))

            // 98. Что такое event listener?
            lo.add(Option(optionId++, 98, "Обработчик событий", true))
            lo.add(Option(optionId++, 98, "Слушатель изменений", false))
            lo.add(Option(optionId++, 98, "Наблюдатель за объектами", false))
            lo.add(Option(optionId++, 98, "Менеджер событий", false))

            // 99. Как работает ===?
            lo.add(Option(optionId++, 99, "Строгое сравнение без приведения типов", true))
            lo.add(Option(optionId++, 99, "Нестрогое сравнение", false))
            lo.add(Option(optionId++, 99, "Сравнение по ссылке", false))
            lo.add(Option(optionId++, 99, "Сравнение значений", false))

            // 100. Что такое JSON?
            lo.add(Option(optionId++, 100, "JavaScript Object Notation", true))
            lo.add(Option(optionId++, 100, "Java Simple Object Notation", false))
            lo.add(Option(optionId++, 100, "JavaScript Oriented Notation", false))
            lo.add(Option(optionId++, 100, "Java Script Object Nodes", false))

            // 101. Как преобразовать в строку?
            lo.add(Option(optionId++, 101, "JSON.stringify()", true))
            lo.add(Option(optionId++, 101, "toString()", true))
            lo.add(Option(optionId++, 101, "String()", true))
            lo.add(Option(optionId++, 101, "parseString()", false))

            // 102. Что такое closure?
            lo.add(Option(optionId++, 102, "Функция с запомненным окружением", true))
            lo.add(Option(optionId++, 102, "Закрытая функция", false))
            lo.add(Option(optionId++, 102, "Анонимная функция", false))
            lo.add(Option(optionId++, 102, "Стрелочная функция", false))

            // 103. Как работает setTimeout?
            lo.add(Option(optionId++, 103, "Выполняет функцию после задержки", true))
            lo.add(Option(optionId++, 103, "Выполняет функцию немедленно", false))
            lo.add(Option(optionId++, 103, "Создает таймер", false))
            lo.add(Option(optionId++, 103, "Останавливает выполнение", false))

            // 104. Что такое this?
            lo.add(Option(optionId++, 104, "Контекст выполнения", true))
            lo.add(Option(optionId++, 104, "Ссылка на функцию", false))
            lo.add(Option(optionId++, 104, "Текущий объект", false))
            lo.add(Option(optionId++, 104, "Глобальный объект", false))

            // 105. Как создать объект?
            lo.add(Option(optionId++, 105, "{}", true))
            lo.add(Option(optionId++, 105, "new Object()", true))
            lo.add(Option(optionId++, 105, "Object.create()", true))
            lo.add(Option(optionId++, 105, "create Object()", false))

            // JavaScript основы (вопросы 106-120)
            // 106. Что такое hoisting?
            lo.add(Option(optionId++, 106, "Поднятие переменных и функций", true))
            lo.add(Option(optionId++, 106, "Оптимизация кода", false))
            lo.add(Option(optionId++, 106, "Сокрытие реализации", false))
            lo.add(Option(optionId++, 106, "Поднятие приоритета", false))

            // 107. Как работает let vs var?
            lo.add(Option(optionId++, 107, "Блочная vs функциональная область видимости", true))
            lo.add(Option(optionId++, 107, "Одинаковое поведение", false))
            lo.add(Option(optionId++, 107, "Глобальная vs локальная область", false))
            lo.add(Option(optionId++, 107, "Статическая vs динамическая область", false))

            // 108. Что такое arrow function?
            lo.add(Option(optionId++, 108, "Стрелочные функции", true))
            lo.add(Option(optionId++, 108, "Функции со стрелками", false))
            lo.add(Option(optionId++, 108, "Асинхронные функции", false))
            lo.add(Option(optionId++, 108, "Быстрые функции", false))

            // 109. Как работает destructuring?
            lo.add(Option(optionId++, 109, "Разбор объектов и массивов", true))
            lo.add(Option(optionId++, 109, "Уничтожение объектов", false))
            lo.add(Option(optionId++, 109, "Декомпозиция данных", false))
            lo.add(Option(optionId++, 109, "Разрушение структур", false))

            // 110. Что такое template literals?
            lo.add(Option(optionId++, 110, "Строки с \${} для переменных", true))
            lo.add(Option(optionId++, 110, "Шаблоны для строк", false))
            lo.add(Option(optionId++, 110, "Литералы шаблонов", false))
            lo.add(Option(optionId++, 110, "Типы данных для шаблонов", false))

            // 111. Как работает spread operator?
            lo.add(Option(optionId++, 111, "... для копирования и объединения", true))
            lo.add(Option(optionId++, 111, "Оператор расширения", false))
            lo.add(Option(optionId++, 111, "Оператор распространения", false))
            lo.add(Option(optionId++, 111, "Оператор копирования", false))

            // 112. Что такое rest parameters?
            lo.add(Option(optionId++, 112, "... для аргументов функции", true))
            lo.add(Option(optionId++, 112, "Параметры отдыха", false))
            lo.add(Option(optionId++, 112, "Оставшиеся параметры", false))
            lo.add(Option(optionId++, 112, "Дополнительные параметры", false))

            // 113. Как создать класс?
            lo.add(Option(optionId++, 113, "class MyClass {}", true))
            lo.add(Option(optionId++, 113, "function MyClass() {}", false))
            lo.add(Option(optionId++, 113, "def class MyClass {}", false))
            lo.add(Option(optionId++, 113, "MyClass class {}", false))

            // 114. Что такое prototype?
            lo.add(Option(optionId++, 114, "Механизм наследования в JS", true))
            lo.add(Option(optionId++, 114, "Прототип объекта", false))
            lo.add(Option(optionId++, 114, "Шаблон для объектов", false))
            lo.add(Option(optionId++, 114, "Базовый класс", false))

            // 115. Как работает inheritance?
            lo.add(Option(optionId++, 115, "extends keyword", true))
            lo.add(Option(optionId++, 115, "implements keyword", false))
            lo.add(Option(optionId++, 115, "inherits keyword", false))
            lo.add(Option(optionId++, 115, "super keyword", false))

            // 116. Что такое callback?
            lo.add(Option(optionId++, 116, "Функция передаваемая как аргумент", true))
            lo.add(Option(optionId++, 116, "Функция обратного вызова", false))
            lo.add(Option(optionId++, 116, "Асинхронная функция", false))
            lo.add(Option(optionId++, 116, "Функция возврата", false))

            // 117. Как работает map()?
            lo.add(Option(optionId++, 117, "Преобразование элементов массива", true))
            lo.add(Option(optionId++, 117, "Создание карты", false))
            lo.add(Option(optionId++, 117, "Фильтрация массива", false))
            lo.add(Option(optionId++, 117, "Сортировка массива", false))

            // 118. Что такое filter()?
            lo.add(Option(optionId++, 118, "Фильтрация элементов массива", true))
            lo.add(Option(optionId++, 118, "Очистка массива", false))
            lo.add(Option(optionId++, 118, "Отбор элементов", false))
            lo.add(Option(optionId++, 118, "Сортировка массива", false))

            // 119. Как работает reduce()?
            lo.add(Option(optionId++, 119, "Агрегация элементов массива", true))
            lo.add(Option(optionId++, 119, "Уменьшение массива", false))
            lo.add(Option(optionId++, 119, "Сжатие массива", false))
            lo.add(Option(optionId++, 119, "Оптимизация массива", false))

            // 120. Что такое Promise?
            lo.add(Option(optionId++, 120, "Обещание будущего результата", true))
            lo.add(Option(optionId++, 120, "Промис объекта", false))
            lo.add(Option(optionId++, 120, "Гарантия выполнения", false))
            lo.add(Option(optionId++, 120, "Асинхронная операция", false))

            // JavaScript асинхронность (вопросы 121-135)
            // 121. Что такое Promise?
            lo.add(Option(optionId++, 121, "Обещание будущего значения", true))
            lo.add(Option(optionId++, 121, "Синхронная операция", false))
            lo.add(Option(optionId++, 121, "Тип данных", false))
            lo.add(Option(optionId++, 121, "Функция обратного вызова", false))

            // 122. Как создать Promise?
            lo.add(Option(optionId++, 122, "new Promise((resolve, reject) => {})", true))
            lo.add(Option(optionId++, 122, "Promise.create()", false))
            lo.add(Option(optionId++, 122, "new Promise()", false))
            lo.add(Option(optionId++, 122, "Promise()", false))

            // 123. Что такое then/catch?
            lo.add(Option(optionId++, 123, "Обработка успеха и ошибок Promise", true))
            lo.add(Option(optionId++, 123, "Методы Promise", false))
            lo.add(Option(optionId++, 123, "Цепочка вызовов", false))
            lo.add(Option(optionId++, 123, "Блоки обработки", false))

            // 124. Как работает async/await?
            lo.add(Option(optionId++, 124, "Синтаксический сахар для Promise", true))
            lo.add(Option(optionId++, 124, "Асинхронные функции", false))
            lo.add(Option(optionId++, 124, "Ожидание результатов", false))
            lo.add(Option(optionId++, 124, "Синхронный код", false))

            // 125. Что такое Promise.all?
            lo.add(Option(optionId++, 125, "Ожидание всех Promise", true))
            lo.add(Option(optionId++, 125, "Параллельное выполнение", false))
            lo.add(Option(optionId++, 125, "Группировка Promise", false))
            lo.add(Option(optionId++, 125, "Массив Promise", false))

            // 126. Как работает Promise.race?
            lo.add(Option(optionId++, 126, "Ожидание первого завершенного Promise", true))
            lo.add(Option(optionId++, 126, "Гонка Promise", false))
            lo.add(Option(optionId++, 126, "Соревнование Promise", false))
            lo.add(Option(optionId++, 126, "Быстрый Promise", false))

            // 127. Что такое Promise.allSettled?
            lo.add(Option(optionId++, 127, "Ожидание всех Promise с результатами", true))
            lo.add(Option(optionId++, 127, "Все Promise завершены", false))
            lo.add(Option(optionId++, 127, "Promise с поселениями", false))
            lo.add(Option(optionId++, 127, "Установленные Promise", false))

            // 128. Как работает Promise.any?
            lo.add(Option(optionId++, 128, "Ожидание первого успешного Promise", true))
            lo.add(Option(optionId++, 128, "Любой Promise", false))
            lo.add(Option(optionId++, 128, "Первый успешный", false))
            lo.add(Option(optionId++, 128, "Быстрый успешный Promise", false))

            // 129. Что такое microtask queue?
            lo.add(Option(optionId++, 129, "Очередь микрозадач", true))
            lo.add(Option(optionId++, 129, "Очередь маленьких задач", false))
            lo.add(Option(optionId++, 129, "Быстрая очередь", false))
            lo.add(Option(optionId++, 129, "Приоритетная очередь", false))

            // 130. Как работает event loop?
            lo.add(Option(optionId++, 130, "Цикл событий", true))
            lo.add(Option(optionId++, 130, "Петля событий", false))
            lo.add(Option(optionId++, 130, "Очередь событий", false))
            lo.add(Option(optionId++, 130, "Цикл обработки", false))

            // 131. Что такое callback queue?
            lo.add(Option(optionId++, 131, "Очередь колбэков", true))
            lo.add(Option(optionId++, 131, "Очередь вызовов", false))
            lo.add(Option(optionId++, 131, "Очередь функций", false))
            lo.add(Option(optionId++, 131, "Очередь задач", false))

            // 132. Как работает setTimeout?
            lo.add(Option(optionId++, 132, "Макрозадача с задержкой", true))
            lo.add(Option(optionId++, 132, "Микрозадача", false))
            lo.add(Option(optionId++, 132, "Синхронная задача", false))
            lo.add(Option(optionId++, 132, "Немедленная задача", false))

            // 133. Что такое setImmediate?
            lo.add(Option(optionId++, 133, "Макрозадача без задержки (Node.js)", true))
            lo.add(Option(optionId++, 133, "Немедленное выполнение", false))
            lo.add(Option(optionId++, 133, "Синхронная задача", false))
            lo.add(Option(optionId++, 133, "Приоритетная задача", false))

            // 134. Как работает process.nextTick?
            lo.add(Option(optionId++, 134, "Микрозадача в Node.js", true))
            lo.add(Option(optionId++, 134, "Следующий тик", false))
            lo.add(Option(optionId++, 134, "Немедленное выполнение", false))
            lo.add(Option(optionId++, 134, "Приоритетная задача", false))

            // 135. Что такое async iterator?
            lo.add(Option(optionId++, 135, "Итератор для асинхронных данных", true))
            lo.add(Option(optionId++, 135, "Асинхронный перебор", false))
            lo.add(Option(optionId++, 135, "Итератор с await", false))
            lo.add(Option(optionId++, 135, "Асинхронный цикл", false))

            quizQuestionRepo.insertAllOptions(lo)
        }

        _uiState.value = _uiState.value.copy(loading = false)
    }

    private fun sendEvent(menuEvent: MenuEvent) {
        viewModelScope.launch {
            _events.send(menuEvent)
        }
    }


    data class UiState(
        val loading:Boolean=true,
        val userLoading:Boolean=false,
        val langButtons:List<LangButton> = emptyList(),
        val processButtons:List<ProcessButton> = emptyList()
    )

    sealed class MenuEvent {
        object NavigateToLogin : MenuEvent()
        object NavigateToQuiz : MenuEvent()
        object NavigateToSettings : MenuEvent()
        object NavigateToStatistics : MenuEvent()
        data class showError(val error:String): MenuEvent()
    }
}