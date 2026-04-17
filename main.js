document.addEventListener('DOMContentLoaded', () => {
    // Dynamic Versioning Logic
    const fetchVersion = async () => {
        try {
            const response = await fetch('version.json');
            const data = await response.json();
            
            // Construimos el nombre del archivo basado en tu formato: TikDownloader V.X.X.apk
            const apkFilename = `TikDownloader V.${data.latestVersionName}.apk`;
            
            // Update Hero Button
            const heroBtn = document.getElementById('heroDownloadBtn');
            const heroLabel = document.getElementById('heroVersionLabel');
            if (heroBtn && heroLabel) {
                heroBtn.href = apkFilename;
                heroLabel.innerText = `V${data.latestVersionName}`;
            }
            
            // Update FAB
            const fabBtn = document.getElementById('fabDownloadBtn');
            if (fabBtn) {
                fabBtn.href = apkFilename;
            }
            
            console.log(`Versión detectada desde AS: ${data.latestVersionName}`);
        } catch (error) {
            console.error('Error al cargar la versión:', error);
        }
    };

    fetchVersion();

    // Scroll Reveal Intersection Observer
    const scrollElements = document.querySelectorAll('.scroll-reveal');

    const elementInView = (el, dividend = 1) => {
        const elementTop = el.getBoundingClientRect().top;
        return (
            elementTop <= (window.innerHeight || document.documentElement.clientHeight) / dividend
        );
    };

    const displayScrollElement = (element) => {
        element.classList.add('visible');
    };

    const hideScrollElement = (element) => {
        element.classList.remove('visible');
    };

    const handleScrollAnimation = () => {
        scrollElements.forEach((el) => {
            if (elementInView(el, 1.25)) {
                displayScrollElement(el);
            } else {
                // Optional: hide element when scroll out
                // hideScrollElement(el);
            }
        });
    };

    // Initially trigger check
    handleScrollAnimation();

    window.addEventListener('scroll', () => {
        handleScrollAnimation();
        
        // FAB visibility logic
        const fab = document.getElementById('fabDownload');
        if (window.scrollY > 300) {
            fab.style.display = 'flex';
            fab.style.opacity = '1';
        }
    });

    // FAQ Accordion Logic
    const faqItems = document.querySelectorAll('.faq-item');
    faqItems.forEach(item => {
        const question = item.querySelector('.faq-question');
        question.addEventListener('click', () => {
            const isActive = item.classList.contains('active');
            
            // Close all items
            faqItems.forEach(i => i.classList.remove('active'));
            
            // Open clicked item if it wasn't active
            if (!isActive) {
                item.classList.add('active');
            }
        });
    });

    // 3D Tilt Effect for Cards
    const cards = document.querySelectorAll('.feature-card');
    cards.forEach(card => {
        card.addEventListener('mousemove', (e) => {
            const rect = card.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;
            
            const centerX = rect.width / 2;
            const centerY = rect.height / 2;
            
            const rotateX = (y - centerY) / 10;
            const rotateY = (centerX - x) / 10;
            
            card.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateY(-10px)`;
        });
        
        card.addEventListener('mouseleave', () => {
            card.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) translateY(0)';
        });
    });

    // Mouse Parallax for Hero Mockup
    const mockup = document.querySelector('.mockup-container');
    if (mockup) {
        document.addEventListener('mousemove', (e) => {
            const x = (window.innerWidth / 2 - e.clientX) / 50;
            const y = (window.innerHeight / 2 - e.clientY) / 50;
            
            mockup.style.transform = `perspective(1000px) rotateX(${5 + y}deg) rotateY(${-5 + x}deg) translateY(${y}px)`;
        });
    }

    // Share Functionality
    const shareBtn = document.getElementById('shareBtn');
    if (shareBtn) {
        shareBtn.addEventListener('click', async () => {
            const shareData = {
                title: 'TikDownloader',
                text: 'Descarga videos de TikTok, Instagram y Facebook sin marca de agua con TikDownloader. ¡Es súper rápido!',
                url: window.location.href
            };

            try {
                if (navigator.share) {
                    await navigator.share(shareData);
                } else {
                    await navigator.clipboard.writeText(window.location.href);
                    const originalText = shareBtn.querySelector('span').innerText;
                    shareBtn.querySelector('span').innerText = '¡LINK COPIADO!';
                    shareBtn.style.borderColor = 'var(--pink)';
                    shareBtn.style.color = 'var(--pink)';
                    
                    setTimeout(() => {
                        shareBtn.querySelector('span').innerText = originalText;
                        shareBtn.style.borderColor = 'var(--cyan)';
                        shareBtn.style.color = 'var(--cyan)';
                    }, 2000);
                }
            } catch (err) {
                console.error('Error sharing:', err);
            }
        });
    }

    // Smooth scroll for anchor links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const targetId = this.getAttribute('href').substring(1);
            const targetElement = document.getElementById(targetId);
            
            if (targetElement) {
                window.scrollTo({
                    top: targetElement.offsetTop,
                    behavior: 'smooth'
                });
            }
        });
    });

    // Neon flicker random variation
    const neonTitle = document.querySelector('.neon-title');
    if (neonTitle) {
        setInterval(() => {
            const random = Math.random();
            if (random > 0.95) {
                neonTitle.style.animation = 'none';
                setTimeout(() => {
                    neonTitle.style.animation = 'neon-flicker 4s infinite alternate';
                }, 50);
            }
        }, 1000);
    }
});
