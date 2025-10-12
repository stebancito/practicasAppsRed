
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

    
    return archivo_bd;
}

/* 
retorna 1 si hay existencias y se decrementa
retorna -1 si no hay existencias
*/
int existencias(json_t *producto, json_t *bd) {
    if (!producto || !bd) {
        fprintf(stderr, "Error: producto o base de datos nulos.\n");
        return -1;
    }

    // Obtener stock actual
    json_t *stock = json_object_get(producto, "stock");
    if (!json_is_integer(stock)) {
        fprintf(stderr, "Error: campo 'stock' no es entero o no existe.\n");
        return -1;
    }

    int stock_actual = json_integer_value(stock);
    if (stock_actual <= 0) {
        printf("El producto está agotado.\n");
        return -1;
    }

    // Reducir stock SOLO en la BD
    int nuevo_stock = stock_actual - 1;
    json_object_set_new(producto, "stock", json_integer(nuevo_stock));

    printf("Stock actualizado en BD: %d\n", nuevo_stock);

    // Guardar cambios en el archivo JSON
    if (json_dump_file(bd, PATH_JSON, JSON_INDENT(4)) != 0) {
        fprintf(stderr, "Error al escribir el archivo JSON en %s\n", PATH_JSON);
        return -1;
    }

    return 1; // éxito
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

/* 
    tipo = 2 si se quiere buscar por id, tipo = 1 si se quiere buscar por tipo, 0 si se quiere buscar por nombre o marca
    nombre_producto puede tomar el valor de un nombre, marca, tipo o id
*/
json_t * buscarProductoPor(const char *nombre_producto, json_t *productos, int tipo){

    const char *buscar = nombre_producto;
    
    size_t index;
    json_t *producto;
    int encontrado = 0;

    json_t *resultado = NULL;
    
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
        }else if(tipo == 2){
            int id_buscar = atoi(buscar);
            json_t *id_producto = json_object_get(producto, "id");
            if (json_is_integer(id_producto) && id_buscar == json_integer_value(id_producto)) {
                encontrado = 1;
                printf("ID encontrado: %s\n", buscar);
                json_array_append(resultado, producto);
                break;
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
    
    json_t *archivo_bd = leerJSON();
    json_t *productos = json_object_get(archivo_bd, "productos");

    json_t *resultado_busqueda;
    json_t *json_final;


    if(productos == NULL){
        printf("%p\n", productos);
        /* strdup hace que un string literal sea dinamico (para no tener conflictos con el free(respuesta)) */
        *respuesta = strdup("{\"error\":\"No se pudo leer la base de datos\"}"); 
        return;
    }

    resultado_busqueda = buscarProductoPor(nombreProducto, productos, tipoBusqueda);
    json_final = prepararJSONRespuesta(resultado_busqueda);
    /* convertimos json_object a texto plano */
    *respuesta = json_dumps(json_final, JSON_INDENT(4));

    json_decref(resultado_busqueda);
    json_decref(json_final);
    json_decref(productos);   // liberar toda la BD
    json_decref(archivo_bd);

    return ;

}

/* Inserta el producto en el carrito */
void agregarProducto(json_t *carrito, json_t *producto) {
    if (!json_is_array(carrito) || !json_is_object(producto)) {
        printf("Error: parámetros inválidos en agregarProducto\n");
        return;
    }

    // Crear copia para el carrito y eliminar stock
    json_t *producto_copia = json_deep_copy(producto);
    json_object_del(producto_copia, "stock");

    // Obtener ID del producto
    json_t *id_json = json_object_get(producto_copia, "id");
    if (!json_is_integer(id_json)) {
        printf("Error: producto sin ID válido\n");
        json_decref(producto_copia);
        return;
    }
    int id_nuevo = json_integer_value(id_json);

    // Verificar si ya está en el carrito
    size_t i;
    json_t *item;
    for (i = 0; i < json_array_size(carrito); i++) {
        item = json_array_get(carrito, i);
        json_t *id_existente_json = json_object_get(item, "id");
        if (json_is_integer(id_existente_json) &&
            json_integer_value(id_existente_json) == id_nuevo) {
            
            // Incrementar cantidad
            json_t *cantidad_json = json_object_get(item, "cantidad");
            int cantidad = json_is_integer(cantidad_json) ? json_integer_value(cantidad_json) : 0;
            json_object_set_new(item, "cantidad", json_integer(cantidad + 1));

            json_decref(producto_copia); // no agregamos copia nueva
            return;
        }
    }

    // Si no estaba, agregamos con cantidad = 1
    json_object_set_new(producto_copia, "cantidad", json_integer(1));
    json_array_append_new(carrito, producto_copia);
}

/* Funcion principal para agregar al carrito */
void agregarCarrito(S_Cliente *cliente, const char *nombreProducto, char **respuesta) {

    json_t *archivo_bd = leerJSON();
    if (!archivo_bd) {
        *respuesta = strdup("{\"error\":\"No se pudo leer la base de datos\"}");
        return;
    }

    json_t *productos = json_object_get(archivo_bd, "productos");
    if (!productos) {
        *respuesta = strdup("{\"error\":\"No se pudo leer la lista de productos\"}");
        json_decref(archivo_bd);
        return;
    }

    // Buscar producto por ID
    json_t *resultado_busqueda = buscarProductoPor(nombreProducto, productos, 2);
    if (!resultado_busqueda) {
        *respuesta = strdup("{\"error\":\"Producto no encontrado\"}");
        json_decref(archivo_bd);
        return;
    }

    json_t *producto_seleccionado = json_array_get(resultado_busqueda, 0);

    /* Verificar existencias y reducir stock en BD */
    if (existencias(producto_seleccionado, archivo_bd) == -1) {
        *respuesta = strdup("{\"error\":\"No hay existencias del producto\"}");

        json_decref(resultado_busqueda);
        json_decref(archivo_bd);
        return;
    }

    agregarProducto(cliente->carrito, producto_seleccionado);

    json_t *json_final = prepararJSONRespuesta(cliente->carrito);
    *respuesta = json_dumps(json_final, JSON_INDENT(4));


    json_decref(resultado_busqueda);
    json_decref(json_final);
    json_decref(archivo_bd);
}
