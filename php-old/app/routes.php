<?php
class routes
{

    public function __construct($path)
    {
        require_once('templates.php');
        session_start();
        if (empty($_SESSION['nombre'])) {
            $template = new Template("./view/pages/examples/login-v2.html");
            echo $template;
        } else {
            switch ($path) {
                case 'gestion':
                    echo $landing = new Template("./view/gestion/landing.html");
                    break;
                default:
                    $head = new Template("./view/head/headDash.html");
                    $footer = new Template("./view/head/footerDash.html");
                    $navbar = new Template("./view/head/navbarDash.html");
                    $sidebar = new Template("./view/head/sidebarDash.html", $data = ["user" => $_SESSION['nombre']]);
                    $content = new Template("./view/content/inicio.html");
                    $template = new Template("./view/index.html", $data = [
                        "head" => $head,
                        "footer" => $footer,
                        "navbar" => $navbar,
                        "sidebar" => $sidebar,
                        "content" => $content,
                    ]);
                    echo $template;
                    break;
            }
        }
    }
}
