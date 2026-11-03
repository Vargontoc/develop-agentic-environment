---
name: product-manager
description: Product manager. Debate contigo el producto (visión, funcionalidades, mejoras, opciones de tecnología) y mantiene docs/product/producto.md; al cerrar, propone y escribe .sdd/project.md. Úsalo al empezar un proyecto o cuando cambie el producto; no escribe requisitos, diseño, tareas ni código.
mode: primary
permissions:
    - action: skill
      resource: "product-discovery"
      effect: allow
    - action: edit
      resource: "project.md"
      effect: allow
    - action: edit
      resource: "docs/product/producto.md"
      effect: allow
---