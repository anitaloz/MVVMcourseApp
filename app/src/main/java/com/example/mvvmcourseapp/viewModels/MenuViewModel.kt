package com.example.mvvmcourseapp.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvvmcourseapp.R
import com.example.mvvmcourseapp.SessionManager
import com.example.mvvmcourseapp.UIhelper.LangButton
import com.example.mvvmcourseapp.UIhelper.ProcessButton
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
        val login = sessionManager.getUserLogin()
        if (sessionManager.isLoggedIn() && login != null) {
            _uiState.value=_uiState.value.copy(userLoading=true)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val dbUser = userRepo.getUserByLogin(login)
                    withContext(Dispatchers.Main) {
                        if (dbUser != null) {
                            sharedViewModel.setUser(dbUser)
                        } else {
                            sharedViewModel.setUser(null) //
                            sessionManager.logout()
                        }
                        _uiState.value=_uiState.value.copy(userLoading=false)
                    }
                } catch (e: Exception) {
                    Log.e("MenuViewModel", "Ошибка загрузки пользователя: ${e.message}")
                    withContext(Dispatchers.Main) {
                        _uiState.value=_uiState.value.copy(loading=false)
                    }
                }
            }
        } else {
            Log.d("MenuViewModel", "No active session or login is null")
            _uiState.value=_uiState.value.copy(loading=false)
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

            // Python - Основы Python (30 вопросов)
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
                QuizQuestion(questionId++, 1, 2, "Что делает функция enumerate()?", "Возвращает индекс и значение", 2),
                QuizQuestion(questionId++, 1, 2, "Как работает оператор with?", "Контекстный менеджер для ресурсов", 2),
                QuizQuestion(questionId++, 1, 2, "Что такое декоратор?", "Функция, изменяющая поведение другой функции", 3),
                QuizQuestion(questionId++, 1, 2, "Как проверить наличие файла?", "os.path.exists() или pathlib", 2),
                QuizQuestion(questionId++, 1, 2, "Что делает метод split()?", "Разбивает строку на список по разделителю", 1),
                QuizQuestion(questionId++, 1, 2, "Как работает zip()?", "Объединяет элементы нескольких последовательностей", 2),
                QuizQuestion(questionId++, 1, 2, "Что такое *args и **kwargs?", "Аргументы переменной длины", 3),
                QuizQuestion(questionId++, 1, 2, "Как создать виртуальное окружение?", "python -m venv myenv", 2),
                QuizQuestion(questionId++, 1, 2, "Что делает метод format() у строк?", "Форматирование строк", 2),
                QuizQuestion(questionId++, 1, 2, "Как читать файл построчно?", "Использовать with open() и цикл for", 2),
                QuizQuestion(questionId++, 1, 2, "Что такое модуль collections?", "Содержит специализированные типы данных", 3),
                QuizQuestion(questionId++, 1, 2, "Как работает map()?", "Применяет функцию к каждому элементу", 2),
                QuizQuestion(questionId++, 1, 2, "Что такое итератор?", "Объект с методами __iter__ и __next__", 3),
                QuizQuestion(questionId++, 1, 2, "Как работает filter()?", "Фильтрует элементы по условию", 2),
                QuizQuestion(questionId++, 1, 2, "Что такое list comprehension?", "Компактное создание списков", 2),
                QuizQuestion(questionId++, 1, 2, "Как преобразовать строку в число?", "int() или float()", 1)
            )
            qql.addAll(pythonBasicQuestions)

            // Python - ООП в Python (30 вопросов)
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
                QuizQuestion(questionId++, 1, 3, "Как создать перечисление?", "from enum import Enum", 2),
                QuizQuestion(questionId++, 1, 3, "Что такое dataclass?", "Автоматическая генерация методов для классов данных", 3),
                QuizQuestion(questionId++, 1, 3, "Как работает __getattr__?", "Вызывается при обращении к несуществующему атрибуту", 3),
                QuizQuestion(questionId++, 1, 3, "Что такое дескриптор?", "Объект с методами __get__, __set__, __delete__", 3),
                QuizQuestion(questionId++, 1, 3, "Как создать синглтон?", "Переопределить __new__ метод", 3),
                QuizQuestion(questionId++, 1, 3, "Что такое миксин?", "Класс для добавления функциональности другим классам", 3),
                QuizQuestion(questionId++, 1, 3, "Как работает __call__?", "Позволяет вызывать объекты как функции", 3),
                QuizQuestion(questionId++, 1, 3, "Что такое инкапсуляция?", "Сокрытие внутренней реализации", 2),
                QuizQuestion(questionId++, 1, 3, "Как создать интерфейс?", "Использовать абстрактные классы", 3),
                QuizQuestion(questionId++, 1, 3, "Что такое композиция?", "Создание объектов из других объектов", 3),
                QuizQuestion(questionId++, 1, 3, "Как работает __slots__?", "Ограничивает атрибуты экземпляра", 3),
                QuizQuestion(questionId++, 1, 3, "Что такое метакласс?", "Класс, создающий классы", 3),
                QuizQuestion(questionId++, 1, 3, "Как создать иммутабельный класс?", "Использовать __slots__ и свойства", 3),
                QuizQuestion(questionId++, 1, 3, "Что такое деструктор?", "__del__ метод", 2),
                QuizQuestion(questionId++, 1, 3, "Как работает @property?", "Создает свойство с getter методом", 2),
                QuizQuestion(questionId++, 1, 3, "Что такое Dunder методы?", "Методы с двойным подчеркиванием", 2)
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

            // Java - Основы Java (30 вопросов)
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
                QuizQuestion(questionId++, 2, 5, "Что такое generics?", "Обобщенные типы", 3),
                QuizQuestion(questionId++, 2, 5, "Как создать список?", "List<String> list = new ArrayList<>()", 2),
                QuizQuestion(questionId++, 2, 5, "Что такое Optional?", "Контейнер для nullable значений", 3),
                QuizQuestion(questionId++, 2, 5, "Как работает Stream API?", "Функциональные операции над коллекциями", 3),
                QuizQuestion(questionId++, 2, 5, "Что такое lambda выражения?", "Анонимные функции", 2),
                QuizQuestion(questionId++, 2, 5, "Как сравнивать объекты?", "Comparable или Comparator", 3),
                QuizQuestion(questionId++, 2, 5, "Что такое reflection?", "Анализ классов во время выполнения", 3),
                QuizQuestion(questionId++, 2, 5, "Как создать файл?", "File класс или NIO", 2),
                QuizQuestion(questionId++, 2, 5, "Что такое serialization?", "Преобразование объекта в байты", 3),
                QuizQuestion(questionId++, 2, 5, "Как работает hashCode()?", "Возвращает хеш-код объекта", 2),
                QuizQuestion(questionId++, 2, 5, "Что такое finalize()?", "Метод вызываемый перед удалением объекта", 3),
                QuizQuestion(questionId++, 2, 5, "Как создать дату?", "LocalDate.now()", 2),
                QuizQuestion(questionId++, 2, 5, "Что такое JDK?", "Java Development Kit", 1),
                QuizQuestion(questionId++, 2, 5, "Как работает instanceof?", "Проверяет тип объекта", 2),
                QuizQuestion(questionId++, 2, 5, "Что такое package-private?", "Доступ в пределах пакета", 2),
                QuizQuestion(questionId++, 2, 5, "Как объявить массив?", "int[] arr или int arr[]", 1)
            )
            qql.addAll(javaBasicQuestions)

            // Java - ООП в Java (30 вопросов)
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
                QuizQuestion(questionId++, 2, 6, "Что такое factory pattern?", "Паттерн создания объектов", 3),
                QuizQuestion(questionId++, 2, 6, "Как работает builder pattern?", "Постепенное создание сложных объектов", 3),
                QuizQuestion(questionId++, 2, 6, "Что такое dependency injection?", "Внедрение зависимостей", 3),
                QuizQuestion(questionId++, 2, 6, "Как создать immutable объект?", "final поля, нет сеттеров", 3),
                QuizQuestion(questionId++, 2, 6, "Что такое композиция?", "Создание объектов из других объектов", 3),
                QuizQuestion(questionId++, 2, 6, "Как работает instanceof?", "Проверка типа объекта", 2),
                QuizQuestion(questionId++, 2, 6, "Что такое covariant return type?", "Возврат подтипа при переопределении", 3),
                QuizQuestion(questionId++, 2, 6, "Как создать анонимный класс?", "new Interface() { методы }", 3),
                QuizQuestion(questionId++, 2, 6, "Что такое marker interface?", "Интерфейс без методов", 3),
                QuizQuestion(questionId++, 2, 6, "Как работает clone()?", "Создание копии объекта", 3),
                QuizQuestion(questionId++, 2, 6, "Что такое sealed classes?", "Классы с ограниченным наследованием", 3),
                QuizQuestion(questionId++, 2, 6, "Как создать record?", "record Point(int x, int y)", 3),
                QuizQuestion(questionId++, 2, 6, "Что такое static factory method?", "Статический метод создания объекта", 3),
                QuizQuestion(questionId++, 2, 6, "Как работает this?", "Ссылка на текущий объект", 2),
                QuizQuestion(questionId++, 2, 6, "Что такое method overloading?", "Методы с одним именем но разными параметрами", 2),
                QuizQuestion(questionId++, 2, 6, "Как создать enum с методами?", "enum с телом и методами", 3)
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

            // JavaScript - Основы JavaScript (30 вопросов)
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
                QuizQuestion(questionId++, 3, 8, "Что такое Promise?", "Обещание будущего результата", 2),
                QuizQuestion(questionId++, 3, 8, "Как работает async/await?", "Синтаксис для работы с асинхронностью", 2),
                QuizQuestion(questionId++, 3, 8, "Что такое event loop?", "Механизм обработки событий", 3),
                QuizQuestion(questionId++, 3, 8, "Как работает call stack?", "Стек вызовов функций", 3),
                QuizQuestion(questionId++, 3, 8, "Что такое callback hell?", "Вложенные колбэки", 2),
                QuizQuestion(questionId++, 3, 8, "Как создать модуль?", "export/import", 2),
                QuizQuestion(questionId++, 3, 8, "Что такое IIFE?", "Немедленно вызываемая функция", 2),
                QuizQuestion(questionId++, 3, 8, "Как работает bind()?", "Привязка контекста", 2),
                QuizQuestion(questionId++, 3, 8, "Что такое apply() и call()?", "Вызов функции с указанным контекстом", 2),
                QuizQuestion(questionId++, 3, 8, "Как работает Object.create()?", "Создание объекта с указанным прототипом", 3),
                QuizQuestion(questionId++, 3, 8, "Что такое Symbol?", "Уникальный и неизменяемый идентификатор", 3),
                QuizQuestion(questionId++, 3, 8, "Как работает Proxy?", "Перехват операций над объектом", 3),
                QuizQuestion(questionId++, 3, 8, "Что такое Reflect?", "Методы для метапрограммирования", 3),
                QuizQuestion(questionId++, 3, 8, "Как работает generator?", "Функция с yield", 3),
                QuizQuestion(questionId++, 3, 8, "Что такое WeakMap?", "Map со слабыми ссылками", 3),
                QuizQuestion(questionId++, 3, 8, "Как работает optional chaining?", "?. для безопасного доступа", 2)
            )
            qql.addAll(jsBasicQuestions)

            // JavaScript - Асинхронность в JS (30 вопросов)
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
                QuizQuestion(questionId++, 3, 9, "Что такое async iterator?", "Итератор для асинхронных данных", 3),
                QuizQuestion(questionId++, 3, 9, "Как работает for await...of?", "Цикл для асинхронных итераторов", 3),
                QuizQuestion(questionId++, 3, 9, "Что такое AbortController?", "Отмена асинхронных операций", 3),
                QuizQuestion(questionId++, 3, 9, "Как работает fetch?", "API для HTTP запросов", 2),
                QuizQuestion(questionId++, 3, 9, "Что такое XMLHttpRequest?", "Старый API для HTTP запросов", 2),
                QuizQuestion(questionId++, 3, 9, "Как работает Web Workers?", "Многопоточность в браузере", 3),
                QuizQuestion(questionId++, 3, 9, "Что такое Service Worker?", "Прокси для сетевых запросов", 3),
                QuizQuestion(questionId++, 3, 9, "Как работает async generator?", "Генератор с асинхронными yield", 3),
                QuizQuestion(questionId++, 3, 9, "Что такое Observable?", "Поток значений (RxJS)", 3),
                QuizQuestion(questionId++, 3, 9, "Как работает debounce?", "Отложенное выполнение функции", 2),
                QuizQuestion(questionId++, 3, 9, "Что такое throttle?", "Ограничение частоты выполнения", 2),
                QuizQuestion(questionId++, 3, 9, "Как создать кастомный EventEmitter?", "Класс с методами on/emit", 3),
                QuizQuestion(questionId++, 3, 9, "Что такое Promise chaining?", "Цепочка then методов", 2),
                QuizQuestion(questionId++, 3, 9, "Как работает async error handling?", "try/catch с await", 2),
                QuizQuestion(questionId++, 3, 9, "Что такое unhandled promise rejection?", "Необработанная ошибка Promise", 2),
                QuizQuestion(questionId++, 3, 9, "Как отменить fetch запрос?", "AbortController + signal", 3)
            )
            qql.addAll(jsAsyncQuestions)

            quizQuestionRepo.insertAllQuizQuestions(qql)
        }

        if(!flo) {
            val lo = mutableListOf<Option>()
            var optionId = 1

            // Опции для Python вопросов (1-25)
            for (questionId in 1..225) {
                when (questionId) {
                    1 -> { // Что выведет print(2 ** 3)?
                        lo.add(Option(optionId++, questionId, "8", true))
                        lo.add(Option(optionId++, questionId, "6", false))
                        lo.add(Option(optionId++, questionId, "9", false))
                        lo.add(Option(optionId++, questionId, "Ошибку", false))
                    }
                    2 -> { // Какой тип данных у значения 3.14?
                        lo.add(Option(optionId++, questionId, "float", true))
                        lo.add(Option(optionId++, questionId, "int", false))
                        lo.add(Option(optionId++, questionId, "double", false))
                        lo.add(Option(optionId++, questionId, "decimal", false))
                    }
                    3 -> { // Как создать пустой список?
                        lo.add(Option(optionId++, questionId, "[]", true))
                        lo.add(Option(optionId++, questionId, "list()", true))
                        lo.add(Option(optionId++, questionId, "{}", false))
                        lo.add(Option(optionId++, questionId, "()", false))
                    }
                    4 -> { // Что делает метод append()?
                        lo.add(Option(optionId++, questionId, "Добавляет элемент в конец списка", true))
                        lo.add(Option(optionId++, questionId, "Удаляет элемент из списка", false))
                        lo.add(Option(optionId++, questionId, "Сортирует список", false))
                        lo.add(Option(optionId++, questionId, "Объединяет два списка", false))
                    }
                    5 -> { // Как получить длину строки?
                        lo.add(Option(optionId++, questionId, "len()", true))
                        lo.add(Option(optionId++, questionId, "length()", false))
                        lo.add(Option(optionId++, questionId, "size()", false))
                        lo.add(Option(optionId++, questionId, "count()", false))
                    }
                    // Java вопросы (16-20)
                    16 -> { // Как объявить переменную в Java?
                        lo.add(Option(optionId++, questionId, "int x = 5;", true))
                        lo.add(Option(optionId++, questionId, "var x = 5;", false))
                        lo.add(Option(optionId++, questionId, "x := 5;", false))
                        lo.add(Option(optionId++, questionId, "let x = 5;", false))
                    }
                    17 -> { // Что такое main метод?
                        lo.add(Option(optionId++, questionId, "Точка входа в программу", true))
                        lo.add(Option(optionId++, questionId, "Метод для вывода текста", false))
                        lo.add(Option(optionId++, questionId, "Конструктор класса", false))
                        lo.add(Option(optionId++, questionId, "Статический блок инициализации", false))
                    }
                    // JavaScript вопросы (21-25)
                    21 -> { // Как объявить переменную?
                        lo.add(Option(optionId++, questionId, "let x = 5;", true))
                        lo.add(Option(optionId++, questionId, "const x = 5;", true))
                        lo.add(Option(optionId++, questionId, "var x = 5;", true))
                        lo.add(Option(optionId++, questionId, "int x = 5;", false))
                    }
                    22 -> { // Что выведет console.log(2 + '2')?
                        lo.add(Option(optionId++, questionId, "'22'", true))
                        lo.add(Option(optionId++, questionId, "4", false))
                        lo.add(Option(optionId++, questionId, "NaN", false))
                        lo.add(Option(optionId++, questionId, "Ошибку", false))
                    }
                    else -> {
                        // Общие опции для остальных вопросов
                        lo.add(Option(optionId++, questionId, "Правильный ответ", true))
                        lo.add(Option(optionId++, questionId, "Неправильный вариант 1", false))
                        lo.add(Option(optionId++, questionId, "Неправильный вариант 2", false))
                        lo.add(Option(optionId++, questionId, "Неправильный вариант 3", false))
                    }
                }
            }

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