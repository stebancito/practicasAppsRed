
#include "funcionesTienda.h"
//#include <jansson.h>
#include <string.h>
#include <stdio.h>
#define PATH_JSON "bd.JSON"

json_t * leerJSON(){
    const char *filename = PATH_JSON;
    json_t *archivo_bd; 
    json_error_t error;

    archivo_bd = json_load_file(filename, 0, &error);
    if (!archivo_bd) {
        fprintf(stderr, "Error en línea %d: %s\n", error.line, error.text);
        return NULL;
    }

    json_decref(archivo_bd);
    
    return archivo_bd;
}

int existencias(json_t *producto, json_t * bd){

    json_t *nombre = json_object_get(producto, "nombre");
    json_t *stock = json_object_get(producto, "stock");


    printf("Stock actual: %d\n", (int)json_integer_value(stock));


    if (json_integer_value(stock) > 0) {
        json_integer_set(stock, json_integer_value(stock) - 1);
        printf("Stock actualizado: %d\n", (int)json_integer_value(stock));

        if (json_dump_file(bd, PATH_JSON, JSON_INDENT(4)) != 0) {
            fprintf(stderr, "Error al escribir el archivo JSON en %s\n", PATH_JSON);
        }

        return 1;

    } else {
        printf("El producto está agotado.\n");
        return -1;
    }

    json_decref(nombre);
    json_decref(stock);
    
}


/* empaqueta un array de productos en un json_object */
json_t* prepararJSONRespuesta(json_t *resultados) {
    json_t *respuesta = json_object();

    if (resultados == NULL || json_array_size(resultados) == 0) {
        json_object_set_new(respuesta, "error", json_string("No se encontraron productos"));
        return respuesta;
    }


    if (json_array_size(resultados) == 1) {
        json_t *producto = json_array_get(resultados, 0);
        json_object_set(respuesta, "producto", producto);
    } else {
        json_object_set(respuesta, "productos", resultados);
    }

    return respuesta;
}


json_t * buscarProductoPor(const char *nombre_producto, json_t *productos, int tipo){
    /* tipo = 1 si se quiere buscar por tipo, 0 si se quiere buscar por nombre o marca*/
    /* nombre_producto puede tomar el valor de un nombre, marca o tipo*/
    const char *buscar = nombre_producto;
    
    size_t index;
    json_t *producto;
    int encontrado = 0;

    json_t *resultado = NULL;
    

    /* por si encontramos varios resultadoas para una misma marca/nombre/tipo*/
    resultado = json_array();

    /* Se busca el producto a traves del array productos */
    json_array_foreach(productos, index, producto) {
        json_t *nombre = json_object_get(producto, "nombre");
        json_t *marca = json_object_get(producto, "marca");

        if(tipo == 0){
            if (json_is_string(nombre) && strcmp(json_string_value(nombre), buscar) == 0) {
                encontrado = 1;
                printf("Producto encontrado: %s\n", buscar);
                json_array_append(resultado, producto);

            } else if(json_is_string(marca) && strcmp(json_string_value(marca), buscar) == 0){
                encontrado = 1;
                printf("Marca encontrada: %s\n", buscar);
                json_array_append(resultado, producto);
            }
        }else if(tipo == 1){
            json_t *tipo_producto = json_object_get(producto, "tipo");
            if (json_is_string(tipo_producto) && strcmp(json_string_value(tipo_producto), buscar) == 0) {
                encontrado = 1;
                printf("Tipo encontrado: %s\n", buscar);
                json_array_append(resultado, producto);
            }
        }
    }

    if (!encontrado) {
        printf("Producto no encontrado: %s\n", buscar);
        json_decref(resultado);
        return NULL;
    }
    return resultado;
}


void buscarProducto(const char *nombreProducto, char **respuesta, int tipoBusqueda){
    
    /* Leemos archivo de BD*/
    json_t *archivo_bd = leerJSON();
    json_t *productos = json_object_get(archivo_bd, "productos");

    json_t *resultado_busqueda;
    json_t *json_final;


    if(productos == NULL){
        *respuesta = "{\"error\":\"No se pudo leer la base de datos\"}";
        return;
    }

    resultado_busqueda = buscarProductoPor(nombreProducto, productos, tipoBusqueda);
    json_final = prepararJSONRespuesta(resultado_busqueda);
    /* convertimos json_object a texto plano */
    *respuesta = json_dumps(json_final, JSON_IDENT(4));

    json_decref(resultado_busqueda);
    json_decref(json_final);
    json_decref(productos);   // liberar toda la BD
    return ;

}


void agregarCarrito(const char *nombreProducto, char **respuesta){
    json_t *carrito = json_array(); // ocupo que carrito sea una especie de variable global para usarlo en demas funciones, chance con un puntero a un carrito

    json_t *archivo_bd = leerJSON();
    json_t *productos = json_object_get(archivo_bd, "productos");
    json_t *json_final, * resultado_busqueda;

    
    if(productos == NULL){
        *respuesta = "{\"error\":\"No se pudo leer la base de datos\"}";
        return;
    }

    resultado_busqueda = buscarProductoPor(nombreProducto, productos, 0); // no se si buscar por id

    if(existencias(resultado_busqueda, archivo_bd) == -1){
        *respuesta = "{\"error\":\"No hay existencias del producto\"}";
        json_decref(resultado_busqueda);
        json_decref(productos);
        return;
    }

    agregarProducto(carrito, resultado_busqueda);

    json_final = prepararJSONRespuesta(resultado_busqueda);

    *respuesta = json_dumps(json_final, JSON_IDENT(4));

    json_decref(resultado_busqueda);
    json_decref(json_final);
    json_decref(productos);   // liberar toda la BD
}

