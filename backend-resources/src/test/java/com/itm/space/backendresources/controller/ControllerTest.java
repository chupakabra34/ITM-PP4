package com.itm.space.backendresources.controller;

import com.itm.space.backendresources.BaseIntegrationTest;
import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.exception.BackendResourcesException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * \* Create by Prekrasnov Sergei
 * \
 */

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "user", password = "root", authorities = "ROLE_MODERATOR")
public class ControllerTest extends BaseIntegrationTest {

    @MockBean
    private Keycloak keycloak;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestExceptionHandler restExceptionHandler;

    @Value("${keycloak.realm}")
    private String realmItm;

    private UserRequest testUserRequest;
    private UserRequest testInvalidUserRequest;
    private RealmResource realmResourceMock;
    private UsersResource usersResourceMock;
    private UserRepresentation userRepresentationMock;
    private UserResource userResourceMock;

    @BeforeEach
    void initNecessaryMocks() {
        testUserRequest = new UserRequest("chupakabra", "chupakabra@gmail.com", "password", "Vladimir", "Putin");
        testInvalidUserRequest = new UserRequest("", "chupakabra@gmail.com", "password", "Vladimir", "Putin");
        realmResourceMock = mock(RealmResource.class);
        usersResourceMock = mock(UsersResource.class);
        userRepresentationMock = mock(UserRepresentation.class);
        userResourceMock = mock(UserResource.class);
    }

    @Test
    public void helloMethodTest_ShouldReturnOk() throws Exception {

        // Выполняется HTTP GET-запрос на адрес /api/users/hello с помощью объекта mvc.
        // mvc представляет собой экземпляр класса MockMvc, который используется для тестирования контроллеров в изоляции от веб-контейнера.
        // Полученный ответ записывается в объект response типа MockHttpServletResponse.
        MockHttpServletResponse response = mvc.perform(get("/api/users/hello")).andReturn().getResponse();

        // Этот assert проверяет, что статус ответа response соответствует ожидаемому значению HttpStatus.OK.value().
        // Значение HttpStatus.OK.value() равно 200, что соответствует успешному выполнению запроса.
        assertEquals(HttpStatus.OK.value(), response.getStatus());

        // Этот assert проверяет, что содержимое ответа response совпадает с ожидаемым значением "user".
        // Тестируемый метод helloMethod должен возвращать строку "user"
        assertEquals("user", response.getContentAsString());
    }

    @Test
    @SneakyThrows
    public void userCreatedTest_ShouldReturnSuccessStatus() {

        // При вызове метода keycloak.realm(realmItm) возвращаем realmResourceMock.
        when(keycloak.realm(realmItm)).thenReturn(realmResourceMock);

        // При вызове метода realmResourceMock.users() возвращаем usersResourceMock.
        when(realmResourceMock.users()).thenReturn(usersResourceMock);

        // При вызове метода usersResourceMock.create(any()) с любым аргументом возвращаем ответ, сигнализирующий об успешном создании пользователя (HTTP статус 201 Created).
        when(usersResourceMock.create(any())).thenReturn(Response.status(Response.Status.CREATED).build());

        // Создание нового случайного идентификатора пользователя и его подставление в мок userRepresentationMock.
        when(userRepresentationMock.getId()).thenReturn(UUID.randomUUID().toString());

        // Выполняем HTTP POST-запроса по адресу "/api/users" с телом запроса testUserRequest,
        // и получение ответа в виде объекта MockHttpServletResponse.
        MockHttpServletResponse response = mvc.perform(requestWithContent(post("/api/users"), testUserRequest))
                .andReturn().getResponse();

        // Проверка, что HTTP статус код ответа равен 200 (HttpStatus.OK).
        assertEquals(HttpStatus.OK.value(), response.getStatus());

        // Проверяем, что метод keycloak.realm(realmItm) был вызван хотя бы один раз.
        verify(keycloak).realm(realmItm);

        // Проверяем, что метод realmResourceMock.users() был вызван хотя бы один раз.
        verify(realmResourceMock).users();

        // Проверка, что метод usersResourceMock.create(any(UserRepresentation.class)) был вызван хотя бы один раз
        // с любым аргументом типа UserRepresentation.
        verify(usersResourceMock).create(any(UserRepresentation.class));
    }

    @Test
    public void getUserByIdTest_ShouldReturnUserIDSuccess() throws Exception {
        UUID userId = UUID.randomUUID();

        // чтобы объект keycloak возвращал realmResourceMock, когда метод realm(realmItm) вызывается с аргументом realmItm
        when(keycloak.realm(realmItm)).thenReturn(realmResourceMock);

        // метод users() возвращал имитацию объекта UsersResource, когда вызывается на объекте realmResourceMock
        when(realmResourceMock.users()).thenReturn(mock(UsersResource.class));

        //  метод get(String.valueOf(userId)) возвращал userResourceMock, когда вызывается на объекте UsersResource, который возвращается методом realmResourceMock.users()
        when(realmResourceMock.users().get(eq(String.valueOf(userId)))).thenReturn(userResourceMock);

        //метод toRepresentation() вызывается на объекте userResourceMock, чтобы он возвращал userRepresentationMock
        when(userResourceMock.toRepresentation()).thenReturn(userRepresentationMock);

        // userRepresentationMock возвращает строковое представление ранее сгенерированного userId, когда вызывается метод getId()
        when(userRepresentationMock.getId()).thenReturn(String.valueOf(userId));

        // Выполняем имитацию HTTP-запроса с использованием объекта mockMvc.
        // Он отправляет GET-запрос на URL "/api/users/{id}" с значением userId в качестве пути.
        // Ожидаем, что статус ответа будет внутренней ошибкой сервера (код состояния HTTP 500). Ответ получается из тестового запроса и сохраняется в переменной response
        MockHttpServletResponse response = mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isInternalServerError())
                .andReturn().getResponse();

        // строка проверяет, что фактический код состояния ответа из объекта response соответствует ожидаемому коду внутренней ошибки сервера (HTTP 500)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());
    }

    @Test
    @SneakyThrows
    public void createUser_ShouldCatchHandleExceptionTest() {

        //Создается объект исключения BackendResourcesException, который имеет сообщение "Backend resources exception occurred" и статус ошибки HTTP INTERNAL_SERVER_ERROR.
        BackendResourcesException exception = new BackendResourcesException("Backend resources exception occurred",
                HttpStatus.INTERNAL_SERVER_ERROR);

        //Вызывается метод handleException объекта restExceptionHandler для обработки исключения exception.
        // Этот метод возвращает ResponseEntity<String> - объект, содержащий информацию об ответе, который был бы отправлен клиенту при возникновении данного исключения.
        ResponseEntity<String> exceptionResponse = restExceptionHandler.handleException(exception);

        // Здесь настраивается поведение мок-объекта keycloak, чтобы при вызове метода realm с аргументом realmItm возвращался объект realmResourceMock
        when(keycloak.realm(realmItm)).thenReturn(realmResourceMock);

        // Настройка поведения мок-объекта realmResourceMock таким образом, чтобы при вызове метода users() возвращался объект usersResourceMock.
        when(realmResourceMock.users()).thenReturn(usersResourceMock);

        // Настраиваем поведение мок-объекта usersResourceMock для метода create, чтобы при вызове с любыми аргументами возвращался Response с HTTP статусом INTERNAL_SERVER_ERROR.
        when(usersResourceMock.create(any())).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());

        //Настройка поведения мок-объекта userRepresentationMock, чтобы при вызове метода getId() возвращалась случайно сгенерированная строка в формате UUID.
        when(userRepresentationMock.getId()).thenReturn(UUID.randomUUID().toString());

        //Выполняется HTTP POST запрос с содержимым testUserRequest на путь "/api/users" с использованием MockMvc,
        // и результатом этого запроса является объект MockHttpServletResponse, который содержит информацию о HTTP-ответе.
        MockHttpServletResponse response = mvc.perform(requestWithContent(post("/api/users"), testUserRequest))
                .andReturn().getResponse();

        //Проверка, что статус код возвращенного exceptionResponse совпадает с INTERNAL_SERVER_ERROR.
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exceptionResponse.getStatusCode());

        // Проверка, что статус код возвращенного response совпадает с INTERNAL_SERVER_ERROR.
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());

        // Проверка, что тело (содержимое) возвращенного exceptionResponse соответствует ожидаемому тексту "Backend resources exception occurred".
        assertEquals("Backend resources exception occurred", exceptionResponse.getBody());
    }

    @Test
    @SneakyThrows
    public void userCreatedTest_ShouldHandleInvalidArgument() {
        // Создается пустой объект типа HashMap, который будет использоваться для хранения ошибок валидации
        Map<String, String> errorMap = new HashMap<>();

        // Эта строка выполняет запрос на создание пользователя с недопустимыми данными (testInvalidUserRequest) в тестовом контроллере mvc.
        // Результат запроса записывается в объект response, который представляет ответ на HTTP запрос.
        try {
            MockHttpServletResponse response = mvc.perform(requestWithContent(post("/api/users"),
                    testInvalidUserRequest)).andReturn().getResponse();
            //Проверяется, что HTTP статус ответа равен 400 (HttpStatus.BAD_REQUEST). Это проверка, что создание пользователя с недопустимыми данными вызывает ошибку "Bad Request"
            assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
            //Если возникло исключение типа MethodArgumentNotValidException, выполняется код в блоке catch. Это исключение генерируется,
            // когда входные аргументы метода не проходят валидацию
        } catch (MethodArgumentNotValidException ex) {
            //Получаются все ошибки валидации (FieldErrors) из исключения ex и помещаются в errorMap. Ключами в errorMap будут имена полей,
            // а значениями - сообщения об ошибках валидации для соответствующих полей
            ex.getBindingResult().getFieldErrors()
                    .forEach(error -> errorMap.put(error.getField(), error.getDefaultMessage()));

            // Вызывается метод handleInvalidArgument из класса restExceptionHandler, для обработки и преобразования
            // исключения ex в объект типа Map<String, String>, содержащий информацию об ошибках валидации
            Map<String, String> response = restExceptionHandler.handleInvalidArgument(ex);

            // Проверяется, что размер объекта response равен 2, что предполагает, что были обнаружены 2 ошибки валидации.
            assertEquals(2, response.size());

            //Проверяется, что в объекте response содержатся ключи "name" и "email", что подразумевает, что ошибки валидации связаны с полями "name" и "email".
            assertTrue(response.containsKey("name"));
            assertTrue(response.containsKey("email"));

            //Проверяется, что для поля "name" значение ошибки валидации соответствует "Name is required", а для поля "email" - "Invalid email format".
            assertEquals("Name is required", response.get("name"));
            assertEquals("Invalid email format", response.get("email"));
        }
    }
}