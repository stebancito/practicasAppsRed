#ifndef funcionesTienda_h
#define funcionesTienda_h

#include <jansson.h>

typedef struct {
    int socket;
    json_t *carrito;
} S_Cliente;

void buscarProducto(const char *nombreProducto, char **respuesta, int tipoBusqueda);
json_t * leerJSON();
int existencias(json_t *producto, json_t * bd);
void agregarProducto(json_t *carrito, json_t *producto);
void agregarCarrito(S_Cliente *cliente, const char *nombreProducto, char **respuesta);
void editarCarrito(S_Cliente *cliente, const char *idProducto, char **respuesta);
json_t* prepararJSONRespuesta(json_t *resultados);
json_t * buscarProductoPor(const char *nombre_producto, json_t *productos, int tipo);

#endif