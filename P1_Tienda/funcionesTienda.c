
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

/*
    Incrementa el stock del producto en la base de datos JSON.
    Retorna 1 si se actualizo correctamente.
    Retorna -1 en caso de error.
*/
int devolverExistencia(const char * id_producto) {


    json_t *bd = leerJSON();
    if (!bd) {
        fprintf(stderr, "Error: no se pudo leer la base de datos.\n");
        return -1;
    }

    json_t *productos = json_object_get(bd, "productos");
    if (!json_is_array(productos)) {
        fprintf(stderr, "Error: 'productos' no es un arreglo.\n");
        json_decref(bd);
        return -1;
    }


    json_t *resultado = buscarProductoPor(id_producto, productos, 2);
    if (!resultado) {
        fprintf(stderr, "Error: producto con ID %s no encontrado.\n", id_producto);
        json_decref(bd);
        return -1;
    }

    // Obtenemos el producto encontrado
    json_t *producto = json_array_get(resultado, 0);
    json_t *stock_json = json_object_get(producto, "stock");

    if (!json_is_integer(stock_json)) {
        fprintf(stderr, "Error: campo 'stock' invalido en el producto.\n");
        json_decref(resultado);
        json_decref(bd);
        return -1;
    }

    int stock_actual = json_integer_value(stock_json);
    int nuevo_stock = stock_actual + 1;
    json_object_set_new(producto, "stock", json_integer(nuevo_stock));

    if (json_dump_file(bd, PATH_JSON, JSON_INDENT(4)) != 0) {
        fprintf(stderr, "Error al escribir la base de datos en %s.\n", PATH_JSON);
        json_decref(resultado);
        json_decref(bd);
        return -1;
    }

    printf("Stock incrementado: %d -> %d (ID: %s)\n", stock_actual, nuevo_stock, id_producto);

    json_decref(resultado);
    json_decref(bd);
    return 1;
}



/* Empaqueta un array de productos en un json_object */
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

    // ya esta en el carrito?
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

    /* Agregamos con cantidad = 1 */
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

    json_t *resultado_busqueda = buscarProductoPor(nombreProducto, productos, 2); // buscar por ID
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

/* 
    Funcion para decrementar o eliminar producto del carrito del cliente.
    Si la cantidad > 1 la reduce.
    Si la cantidad = 1 elimina el producto del carrito.
    Devuelve la existencia al stock de la BD.
*/
void editarCarrito(S_Cliente *cliente, const char *idProducto, char **respuesta) {
    if (!cliente || !cliente->carrito) {
        *respuesta = strdup("{\"error\":\"Carrito no disponible\"}");
        return;
    }

    int id_a_decrementar = atoi(idProducto);
    if (id_a_decrementar <= 0) {
        *respuesta = strdup("{\"error\":\"ID inválido\"}");
        return;
    }

    size_t i;
    json_t *item;
    int encontrado = 0;

    for (i = 0; i < json_array_size(cliente->carrito); i++) {
        item = json_array_get(cliente->carrito, i);
        json_t *id_item = json_object_get(item, "id");

        if (json_is_integer(id_item) && json_integer_value(id_item) == id_a_decrementar) {
            encontrado = 1;

            json_t *cantidad_json = json_object_get(item, "cantidad");
            int cantidad = json_is_integer(cantidad_json) ? json_integer_value(cantidad_json) : 0;

            if (cantidad > 1) {
                json_object_set_new(item, "cantidad", json_integer(cantidad - 1));
            } else {
                json_array_remove(cliente->carrito, i);
            }

            /* Incrementar el stock en la BD */
            if (devolverExistencia(idProducto) == -1) {
                *respuesta = strdup("{\"error\":\"Error al actualizar stock en la BD\"}");
                return;
            }

            break;
        }
    }

    if (!encontrado) {
        *respuesta = strdup("{\"error\":\"Producto no encontrado en el carrito\"}");
        return;
    }

    json_t *json_final = prepararJSONRespuesta(cliente->carrito);
    *respuesta = json_dumps(json_final, JSON_INDENT(4));
    json_decref(json_final);
}


json_t *generarDetalleProducto(json_t *producto, double *total_general) {
    if (!producto || !total_general) return NULL;

    /* Sacamos los datos del producto*/
    json_t *nombre_json = json_object_get(producto, "nombre");
    json_t *marca_json = json_object_get(producto, "marca");
    json_t *precio_json = json_object_get(producto, "precio");
    json_t *cantidad_json = json_object_get(producto, "cantidad");

    if (!json_is_string(nombre_json) || !json_is_string(marca_json) ||
        !json_is_number(precio_json) || !json_is_integer(cantidad_json)) {
        fprintf(stderr, "Error: datos inválidos en un producto del carrito.\n");
        return NULL;
    }

    const char *nombre = json_string_value(nombre_json);
    const char *marca = json_string_value(marca_json);
    double precio = json_number_value(precio_json);
    int cantidad = json_integer_value(cantidad_json);

    double subtotal = precio * cantidad;
    *total_general += subtotal;

    json_t *producto_ticket = json_object();
    json_object_set_new(producto_ticket, "nombre", json_string(nombre));
    json_object_set_new(producto_ticket, "marca", json_string(marca));
    json_object_set_new(producto_ticket, "cantidad", json_integer(cantidad));
    json_object_set_new(producto_ticket, "precio_unitario", json_real(precio));
    json_object_set_new(producto_ticket, "subtotal", json_real(subtotal));

    return producto_ticket;
}


void generarTicket(S_Cliente *cliente, char **respuesta) {
    if (!cliente || !cliente->carrito || json_array_size(cliente->carrito) == 0) {
        *respuesta = strdup("{\"error\":\"El carrito está vacío o no disponible\"}");
        return;
    }

    json_t *ticket = json_object();
    json_t *items = json_array();

    double total_general = 0.0;

    /* procesamiento de cada producto del carrito */
    for (size_t i = 0; i < json_array_size(cliente->carrito); i++) {
        json_t *item = json_array_get(cliente->carrito, i);
        json_t *obj = generarDetalleProducto(item, &total_general);
        if (obj) json_array_append_new(items, obj);
    }

    json_object_set_new(ticket, "productos", items);
    json_object_set_new(ticket, "total", json_real(total_general));
    json_object_set_new(ticket, "mensaje", json_string("Compra finalizada con exito"));

    *respuesta = json_dumps(ticket, JSON_INDENT(4));


    json_array_clear(cliente->carrito);

    json_decref(ticket);
}


/*
    Devuelve las existencias de todos los productos en el carrito
    cuando el cliente sale sin comprar.
*/
void devolverProductosCarrito(json_t *carrito) {
    if (!carrito || !json_is_array(carrito)) return;

    size_t index;
    json_t *producto;
    json_array_foreach(carrito, index, producto) {
        json_t *id_json = json_object_get(producto, "id");
        if (json_is_integer(id_json)) {
            char id_str[20];
            snprintf(id_str, sizeof(id_str), "%lld", json_integer_value(id_json));
            devolverExistencia(id_str);
        }
    }
}

