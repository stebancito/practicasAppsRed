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

void procesarPeticion(char *buffer, int nbytes, int cliente_s);

int main(){

    

    int server_s, cliente_s; // fd para el socket servidor y cliente
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
        
        printf("Socket creado y bindeado con éxito en puerto %s\n", PUERTO);
        break;
    }

    if (lista_aux == NULL){
        fprintf(stderr, "No se pudo crear y bindear el socket\n");
        return 2;
    }

    freeaddrinfo(lista_aux);

    if (listen(server_s, BACKLOG) == -1) {
        perror("listen");
        close(server_s);
        exit(1);
    }
    printf("Servidor escuchando en el puerto %s...\n", PUERTO);

    /* Guardamos direccion del cliente*/
    struct sockaddr_storage cliente_addr;
    socklen_t cliente_tam_addr = sizeof cliente_addr;

        while(1){
            printf("Esperando conexiones...\n");
            cliente_s = accept(server_s, (struct sockaddr *)&cliente_addr, &cliente_tam_addr);
        
            if (cliente_s == -1) {
                perror("accept");
                continue;
            }

            printf("Cliente conectado!\n");

            int nbytes;
            while((nbytes = recv(cliente_s, buffer, BUFF-1, 0)) > 0){
                buffer[nbytes] = '\0';
                printf("Cliente dice: %s\n", buffer);

                if(strncmp(buffer, "SALIR", 5) == 0){
                    printf("Cliente pidió salir.\n");
                    break;
                }else{
                    procesarPeticion(buffer, nbytes, cliente_s);
                }
            }

            if (nbytes == 0) {
                printf("El cliente cerró la conexión.\n");
            } else if (nbytes == -1) {
                perror("recv");
            }


            close(cliente_s);
            printf("Conexión con cliente cerrada.\n");
        }    

        close(server_s);
        return 0;
}

void procesarPeticion(char *buffer, int nbytes, int cliente_s){

    if(strncmp(buffer, "BUSCAR", 6) == 0){

        /* Hacemos un eco con la accion que nos dijo */
        char respuesta[] = "{\"accion\":\"BUSCAR\",\"estado\":\"OK\",\"mensaje\":\"Envie el nombre del producto\"}";
        send(cliente_s, respuesta, strlen(respuesta), 0);

            printf("Producto/marca a buscar: %s\n", buffer+7);


            char *resultado = NULL;
            buscarProducto(buffer + 7, &resultado);
            
            printf("Resultado de la búsqueda: %s\n", resultado);

            send(cliente_s, resultado, strlen(resultado), 0);
            free(resultado);
    }
    // else if(strcmp(buffer, "LISTAR") == 0){ LISTAR LOS PRODUTOS POR TIPO, ME VA A LLEGAR LISTAR + TIPO
    //     char respuesta[] = listarArticulos(); 
    //     send(cliente_s, respuesta, strlen(respuesta), 0);
    // } else if(strcmp(buffer, "AGREGAR") == 0){ ME VA A MANDAR AGREGAR + NOMBRE Y YO AGREGARE AL CARRITO
    //     char respuesta[] = agregarProducto();
    //     send(cliente_s, respuesta, strlen(respuesta), 0); 
    // } else if(strcmp(buffer, "EDITAR") == 0){ MANDARLE SU CARRITO EN FORMA DE ARREGLO DONDE SE VEAN LOS INDICES PARA QUE EL ESCOJA
    //     char respuesta[] = borrarProducto();
    //     send(cliente_s, respuesta, strlen(respuesta), 0);
    // }
    else{
        char respuesta[] = "{\"error\":\"Comando no reconocido intente de nuevo\"}";
        send(cliente_s, respuesta, strlen(respuesta), 0);
    }
}