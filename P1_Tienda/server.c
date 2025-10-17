/* 
Servidor 

gcc server.c funcionesTienda.c -o servidor -ljansson
*/

#include <netdb.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <stdlib.h>
#include "funcionesTienda.h"

#define PUERTO "8080"
#define BACKLOG 3
#define BUFF    3000


void procesarPeticion(char *buffer, int nbytes, S_Cliente cliente);

int main(){

    
    int server_s;            // fd para el socket servidor
    char buffer[BUFF];       // buffer para datos entrantes

    struct addrinfo s_configs; // configuraciones del servidor
    struct addrinfo *lista, *lista_aux; 

    memset(&s_configs, 0, sizeof(s_configs));

    s_configs.ai_family = AF_UNSPEC;     // AF_INET o AF_INET6 
    s_configs.ai_socktype = SOCK_STREAM;
    s_configs.ai_flags = AI_PASSIVE;
    s_configs.ai_protocol = 0;

    int rget;

    if((rget = getaddrinfo(NULL, PUERTO, &s_configs, &lista)) != 0){
        fprintf(stderr, "getaddrinfo : %s\n", gai_strerror(rget));
        return 1;
    }

    /* Recorremos lista de targetas de red */
    for(lista_aux = lista; lista_aux != NULL; lista_aux = lista_aux->ai_next){
        if((server_s = socket(lista_aux->ai_family, lista_aux->ai_socktype, lista_aux->ai_protocol)) == -1){
            perror("Server : socket");
            continue;
        }

        int si = 1;
        if(setsockopt(server_s, SOL_SOCKET, SO_REUSEADDR, &si, sizeof(int)) == -1){
            perror("setsockopt");
            exit(1);
        }

        if(bind(server_s, lista_aux->ai_addr, lista_aux->ai_addrlen) == -1){
            perror("Server : bind");
            close(server_s);
            continue;
        }
        
        printf("\033[0;32mS: Socket creado y bindeado con éxito en puerto %s\n\033[0m", PUERTO);
        break;
    }

    if (lista_aux == NULL){
        fprintf(stderr, "\033[0;30mNo se pudo crear y bindear el socket\n\033[0m");
        return 2;
    }

    freeaddrinfo(lista_aux);

    if (listen(server_s, BACKLOG) == -1) {
        perror("listen");
        close(server_s);
        exit(1);
    }
    printf("S: Servidor escuchando en el puerto %s...\n", PUERTO);

    /* Guardamos direccion del cliente */
    struct sockaddr_storage cliente_addr;
    socklen_t cliente_tam_addr = sizeof cliente_addr;

        while(1){
            printf("S: Esperando conexiones...\n\n");
            S_Cliente cliente;
            cliente.socket =  accept(server_s, (struct sockaddr *)&cliente_addr, &cliente_tam_addr);
        
            if (cliente.socket == -1) {
                perror("accept");
                continue;
            }

            printf("\033[0;32mS: Cliente conectado!\n\033[0m");
            cliente.carrito = json_array(); // carrito vacío


            int nbytes;
            while((nbytes = recv(cliente.socket, buffer, BUFF-1, 0)) > 0){
                buffer[nbytes] = '\0';
                printf("\tCliente dice: %s\n", buffer);

                if(strncmp(buffer, "SALIR", 5) == 0){
                    printf("S: Cliente pidió salir.\n");
                    devolverProductosCarrito(cliente.carrito);
                    break;
                }else{
                    procesarPeticion(buffer, nbytes, cliente);
                }
            }

            if (nbytes == 0) {
                printf("\n\033[31mS: El cliente cerró la conexión.\n\033[0m");
                devolverProductosCarrito(cliente.carrito);
            } else if (nbytes == -1) {
                perror("recv");
            }

            close(cliente.socket);
            json_decref(cliente.carrito);
            printf("S: Conexión con cliente cerrada.\n");
            printf("--------------------------------------------------\n\n");


        }    

        close(server_s);
        return 0;
}

void procesarPeticion(char *buffer, int nbytes, S_Cliente cliente){

    json_t ** carrito = NULL;

    if(strncmp(buffer, "BUSCAR", 6) == 0){


        printf("Producto/marca a buscar: %s\n", buffer+7);


        char *resultado = NULL;
        buscarProducto(buffer + 7, &resultado, 0); // 0 es el tipo de busqueda por nom/marca
        
        printf("Resultado de la búsqueda: %s\n", resultado);
        
        if(send(cliente.socket, resultado, strlen(resultado), 0) == -1){
            perror("send buscar");
        }
        free(resultado);
    }
    else if(strncmp(buffer, "LISTAR", 6) == 0){ ////LISTAR LOS PRODUTOS POR TIPO, ME VA A LLEGAR LISTAR + TIPO

        printf("Listando articulos por tipo: %s\n", buffer+7);

        char *resultado = NULL;
        buscarProducto(buffer + 7, &resultado, 1); // 1 es el tipo de busqueda por tipo

        printf("Lista de productos: %s\n", resultado);
        if(send(cliente.socket, resultado, strlen(resultado), 0) == -1){
            perror("send listar");
        }
        free(resultado);
    }
    else if(strncmp(buffer, "AGREGAR", 7) == 0){ // ME VA A MANDAR AGREGAR + id Y YO AGREGARE AL CARRITO

        printf("Agregando articulo al carrito, ID: %s\n", buffer+8);

        char * resultado = NULL; 
        agregarCarrito(&cliente, buffer + 8, &resultado); 
        printf("Resultado de agregar al carrito: %s\n", resultado);

        if(send(cliente.socket, resultado, strlen(resultado), 0) == -1)
            perror("send agregar");
        
        free(resultado);


    } 
    else if(strcmp(buffer, "EDITAR") == 0) {
        char *resultado = NULL;

        /* Enviamos su carrito actual */
        if(json_array_size(cliente.carrito) == 0){
            resultado = strdup("{\"error\":\"Debes tener un carrito para editarlo!\"}");
            if(send(cliente.socket, resultado, strlen(resultado), 0) == -1){

                perror("send editar");
                free(resultado);
            }
            return;  // ISAAC GESTIONA ESTO EN EL CLIENTE PARA QUE NO ME ENVIES EL ID DE UN PRODUCTO SI NO TENGO NADA EN EL CARRITO
        }
        json_t *carrito_actual = prepararJSONRespuesta(cliente.carrito);
        resultado = json_dumps(carrito_actual, JSON_INDENT(4));


        if(send(cliente.socket, resultado, strlen(resultado), 0) == -1)
            perror("send editar");
        
        free(resultado);
        json_decref(carrito_actual);

        /* Recibimos el id del producto a decrementar */
        int nbytes;
        if ((nbytes = recv(cliente.socket, buffer, BUFF-1, 0)) < 0) {
            perror("recv editar");
            return;
        }
        if (nbytes == 0) {
            printf("Cliente desconectado durante edición\n");
            return;
        }

        buffer[nbytes] = '\0';
        printf("Cliente decrementar ID: %s\n", buffer);

        editarCarrito(&cliente, buffer, &resultado);

        printf("Resultado de editar carrito: %s\n", resultado);
        if(send(cliente.socket, resultado, strlen(resultado), 0) == -1)
            perror("send editar");
        
        free(resultado);
    }
    else if(strncmp(buffer, "COMPRAR", 7) == 0){
        printf("Finalizando compra y generando ticket\n");

        char * resultado = NULL; 
        generarTicket(&cliente, &resultado); 
        printf("Ticket generado: %s\n", resultado);

        if(send(cliente.socket, resultado, strlen(resultado), 0) == -1)
            perror("send ticket");
        
        free(resultado);

    }
    else{
        char respuesta[] = "{\"error\":\"Comando no reconocido intente de nuevo\"}";
        send(cliente.socket, respuesta, strlen(respuesta), 0);
    }
}